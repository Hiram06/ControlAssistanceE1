package com.example.controlassistance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AlumnoEscanearInscripcionActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var scanned = false
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alumno_qr) // Reutiliza el mismo layout del escáner

        cameraExecutor = Executors.newSingleThreadExecutor()
        findViewById<Button>(R.id.btnVolver).setOnClickListener { finish() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(
                    findViewById<PreviewView>(R.id.previewView).surfaceProvider
                )
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                if (!scanned) processImage(imageProxy) else imageProxy.close()
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error cámara: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        BarcodeScanning.getClient(options).process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty() && !scanned) {
                    val raw = barcodes[0].rawValue
                    if (raw != null) { scanned = true; handleQRCode(raw) }
                }
            }
            .addOnFailureListener { }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun handleQRCode(qrValue: String) {
        val parts = qrValue.split("|")

        // Validar que es un QR de inscripción (no de asistencia)
        if (parts.size != 4 || parts[0] != "INSCRIPCION") {
            runOnUiThread {
                Toast.makeText(this,
                    "QR inválido. Pide al maestro que muestre el código de inscripción.",
                    Toast.LENGTH_LONG).show()
            }
            scanned = false
            return
        }

        val grupoId  = parts[1]
        val tokenQR  = parts[3]
        val alumnoUid = auth.currentUser?.uid ?: run { scanned = false; return }

        // Verificar si ya está inscrito en este grupo
        db.child("grupos").child(grupoId).child("alumnos").child(alumnoUid).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    runOnUiThread {
                        Toast.makeText(this,
                            "Ya estás inscrito en este grupo.",
                            Toast.LENGTH_LONG).show()
                        finish()
                    }
                    return@addOnSuccessListener
                }

                // Verificar que el token sigue activo
                db.child("qr_inscripcion").child(grupoId).get()
                    .addOnSuccessListener { qrSnap ->
                        val tokenActivo = qrSnap.child("token").getValue(String::class.java) ?: ""
                        val usado = qrSnap.child("usado").getValue(Boolean::class.java) ?: true

                        if (tokenActivo != tokenQR || usado) {
                            runOnUiThread {
                                Toast.makeText(this,
                                    "Este QR ya no es válido. Pide al maestro que muestre el nuevo código.",
                                    Toast.LENGTH_LONG).show()
                            }
                            scanned = false
                            return@addOnSuccessListener
                        }

                        // Obtener nombre del alumno
                        db.child("usuarios").child(alumnoUid).child("nombre").get()
                            .addOnSuccessListener { nombreSnap ->
                                val nombre = nombreSnap.getValue(String::class.java) ?: "Alumno"

                                // Inscribir al alumno en el grupo
                                db.child("grupos").child(grupoId)
                                    .child("alumnos").child(alumnoUid).setValue(true)
                                    .addOnSuccessListener {
                                        // Marcar token como usado → maestro ve confirmación y genera nuevo QR
                                        db.child("qr_inscripcion").child(grupoId).updateChildren(mapOf(
                                            "usado"       to true,
                                            "alumnoNombre" to nombre
                                        ))
                                        runOnUiThread {
                                            Toast.makeText(this,
                                                "¡Inscripción exitosa! Bienvenido al grupo, $nombre.",
                                                Toast.LENGTH_LONG).show()
                                            finish()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        runOnUiThread {
                                            Toast.makeText(this,
                                                "Error al inscribirse: ${e.message}",
                                                Toast.LENGTH_LONG).show()
                                        }
                                        scanned = false
                                    }
                            }
                    }
                    .addOnFailureListener {
                        runOnUiThread {
                            Toast.makeText(this, "Error de conexión. Intenta de nuevo.",
                                Toast.LENGTH_LONG).show()
                        }
                        scanned = false
                    }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}