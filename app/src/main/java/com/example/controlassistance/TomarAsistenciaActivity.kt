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

class TomarAsistenciaActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private var alumnos = mutableListOf<Pair<String, String>>()
    private var currentIndex = 0
    private lateinit var grupoId: String
    private lateinit var maestroId: String

    private lateinit var tvAlumnoActual: TextView
    private lateinit var ivQR: ImageView
    private lateinit var btnSiguiente: Button
    private lateinit var btnNuevoQR: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tomar_asistencia)

        grupoId   = intent.getStringExtra("grupoId") ?: ""
        val grupoNombre = intent.getStringExtra("grupoNombre") ?: ""
        maestroId = auth.currentUser?.uid ?: ""

        tvAlumnoActual = findViewById(R.id.tvAlumnoActual)
        ivQR           = findViewById(R.id.ivQRMaestro)
        btnSiguiente   = findViewById(R.id.btnSiguienteAlumno)
        btnNuevoQR     = findViewById(R.id.btnNuevoQR)

        findViewById<TextView>(R.id.tvTituloAsistencia).text = "Tomar asistencia: $grupoNombre"

        cargarAlumnos()

        btnSiguiente.setOnClickListener {
            currentIndex++
            if (currentIndex < alumnos.size) {
                mostrarQRAlumno(currentIndex)
            } else {
                Toast.makeText(this, "Asistencia completada para todos los alumnos", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        btnNuevoQR.setOnClickListener {
            if (alumnos.isNotEmpty() && currentIndex < alumnos.size) {
                mostrarQRAlumno(currentIndex)
            }
        }
    }

    private fun cargarAlumnos() {
        db.child("grupos").child(grupoId).child("alumnos")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    alumnos.clear()
                    val uidList = snapshot.children.map { it.key ?: "" }.filter { it.isNotEmpty() }
                    if (uidList.isEmpty()) {
                        Toast.makeText(this@TomarAsistenciaActivity, "No hay alumnos en este grupo", Toast.LENGTH_LONG).show()
                        return
                    }
                    var loaded = 0
                    for (uid in uidList) {
                        db.child("usuarios").child(uid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnap: DataSnapshot) {
                                    val nombre = userSnap.child("nombre").getValue(String::class.java) ?: uid
                                    alumnos.add(Pair(uid, nombre))
                                    loaded++
                                    if (loaded == uidList.size) {
                                        alumnos.sortBy { it.second }
                                        currentIndex = 0
                                        mostrarQRAlumno(0)
                                    }
                                }
                                override fun onCancelled(e: DatabaseError) { loaded++ }
                            })
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun mostrarQRAlumno(index: Int) {
        val (uid, nombre) = alumnos[index]
        tvAlumnoActual.text = "Alumno ${index + 1}/${alumnos.size}: $nombre"

        val qrContent = "ASISTENCIA|$grupoId|$maestroId|$uid"

        try {
            val writer = MultiFormatWriter()
            val matrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 600, 600)
            val encoder = BarcodeEncoder()
            val bitmap: Bitmap = encoder.createBitmap(matrix)
            ivQR.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Error generando QR", Toast.LENGTH_SHORT).show()
        }
    }
}