package com.example.controlassistance

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.util.UUID

class InscripcionQRActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseDatabase.getInstance().reference
    private lateinit var grupoId: String
    private lateinit var maestroId: String
    private lateinit var ivQR: ImageView
    private lateinit var tvEstado: TextView
    private var tokenActual: String = ""
    private var tokenListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inscripcion_qr)

        grupoId   = intent.getStringExtra("grupoId") ?: ""
        maestroId = auth.currentUser?.uid ?: ""
        val grupoNombre = intent.getStringExtra("grupoNombre") ?: ""

        ivQR     = findViewById(R.id.ivQRInscripcion)
        tvEstado = findViewById(R.id.tvEstadoInscripcion)

        findViewById<TextView>(R.id.tvTituloInscripcion).text =
            "Inscripción: $grupoNombre"

        findViewById<Button>(R.id.btnVolverInscripcion).setOnClickListener {
            limpiarToken()
            finish()
        }

        generarNuevoToken()
        escucharTokenUsado()
    }

    private fun generarNuevoToken() {
        tokenActual = UUID.randomUUID().toString()

        // Prefijo INSCRIPCION para distinguirlo del QR de asistencia
        val qrContent = "INSCRIPCION|$grupoId|$maestroId|$tokenActual"

        db.child("qr_inscripcion").child(grupoId).setValue(mapOf(
            "token"    to tokenActual,
            "grupoId"  to grupoId,
            "maestroId" to maestroId,
            "usado"    to false
        ))

        try {
            val matrix  = MultiFormatWriter().encode(qrContent, BarcodeFormat.QR_CODE, 600, 600)
            val bitmap: Bitmap = BarcodeEncoder().createBitmap(matrix)
            ivQR.setImageBitmap(bitmap)
            tvEstado.text = "Esperando que un alumno escanee el código..."
        } catch (e: Exception) {
            Toast.makeText(this, "Error generando QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun escucharTokenUsado() {
        tokenListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usado = snapshot.child("usado").getValue(Boolean::class.java) ?: false
                val alumnoNombre = snapshot.child("alumnoNombre").getValue(String::class.java) ?: ""
                if (usado) {
                    tvEstado.text = "✓ $alumnoNombre se inscribió correctamente. Generando nuevo QR..."
                    generarNuevoToken()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("qr_inscripcion").child(grupoId).addValueEventListener(tokenListener!!)
    }

    private fun limpiarToken() {
        db.child("qr_inscripcion").child(grupoId).removeValue()
        tokenListener?.let {
            db.child("qr_inscripcion").child(grupoId).removeEventListener(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        limpiarToken()
    }
}