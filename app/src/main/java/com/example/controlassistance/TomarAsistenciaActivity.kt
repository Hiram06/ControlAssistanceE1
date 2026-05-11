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

class TomarAsistenciaActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_tomar_asistencia)

        grupoId   = intent.getStringExtra("grupoId") ?: ""
        maestroId = auth.currentUser?.uid ?: ""
        val grupoNombre = intent.getStringExtra("grupoNombre") ?: ""

        ivQR    = findViewById(R.id.ivQRMaestro)
        tvEstado = findViewById(R.id.tvEstadoAsistencia)
        findViewById<TextView>(R.id.tvTituloAsistencia).text = "Tomando asistencia: $grupoNombre"
        findViewById<Button>(R.id.btnVolverAsistencia).setOnClickListener {
            limpiarToken()
            finish()
        }

        generarNuevoToken()
        escucharTokenUsado()
    }

    private fun generarNuevoToken() {
        tokenActual = UUID.randomUUID().toString()
        val qrContent = "ASISTENCIA|$grupoId|$maestroId|$tokenActual"

        db.child("qr_activo").child(grupoId).setValue(mapOf(
            "token"     to tokenActual,
            "grupoId"   to grupoId,
            "maestroId" to maestroId,
            "usado"     to false
        ))

        try {
            val writer  = MultiFormatWriter()
            val matrix  = writer.encode(qrContent, BarcodeFormat.QR_CODE, 600, 600)
            val encoder = BarcodeEncoder()
            val bitmap: Bitmap = encoder.createBitmap(matrix)
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
                    tvEstado.text = "✓ $alumnoNombre registró su asistencia. Generando nuevo QR..."
                    generarNuevoToken()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("qr_activo").child(grupoId).addValueEventListener(tokenListener!!)
    }

    private fun limpiarToken() {
        db.child("qr_activo").child(grupoId).removeValue()
        tokenListener?.let {
            db.child("qr_activo").child(grupoId).removeEventListener(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        limpiarToken()
    }
}