package com.example.controlassistance

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class AdminInscribirAlumnosActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance().reference
    private lateinit var grupoId: String
    private lateinit var llSinInscribir: LinearLayout
    private lateinit var llInscritos: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_inscribir)

        grupoId       = intent.getStringExtra("grupoId") ?: ""
        val grupoNombre = intent.getStringExtra("grupoNombre") ?: ""

        llSinInscribir = findViewById(R.id.llSinInscribir)
        llInscritos    = findViewById(R.id.llInscritos)

        findViewById<TextView>(R.id.tvTituloInscribir).text = grupoNombre
        findViewById<Button>(R.id.btnVolverInscribir).setOnClickListener { finish() }

        cargarAlumnos()
    }

    private fun cargarAlumnos() {
        db.child("usuarios").orderByChild("tipo").equalTo("Alumno")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    db.child("grupos").child(grupoId).child("alumnos")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(inscritosSnap: DataSnapshot) {
                                val inscritos = inscritosSnap.children.mapNotNull { it.key }.toSet()

                                llSinInscribir.removeAllViews()
                                llInscritos.removeAllViews()

                                for (snap in snapshot.children) {
                                    val uid    = snap.key ?: continue
                                    val nombre = snap.child("nombre").getValue(String::class.java) ?: ""
                                    val matric = snap.child("matricula").getValue(String::class.java) ?: ""

                                    if (inscritos.contains(uid)) {
                                        agregarFila(llInscritos, uid, nombre, matric, true)
                                    } else {
                                        agregarFila(llSinInscribir, uid, nombre, matric, false)
                                    }
                                }
                            }
                            override fun onCancelled(e: DatabaseError) {}
                        })
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun agregarFila(layout: LinearLayout, uid: String, nombre: String, matricula: String, inscrito: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 8, 12, 8)
            setBackgroundColor(if (inscrito) android.graphics.Color.parseColor("#DDFFDD")
            else android.graphics.Color.WHITE)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, 6) }
            layoutParams = params
        }

        val tvNombre = TextView(this).apply {
            text = "$nombre\n$matricula"
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btn = Button(this).apply {
            text = if (inscrito) "Quitar" else "Inscribir"
            setBackgroundColor(if (inscrito) android.graphics.Color.parseColor("#C0392B")
            else android.graphics.Color.parseColor("#27AE60"))
            setTextColor(android.graphics.Color.WHITE)
            textSize = 12f
            setPadding(16, 8, 16, 8)
            setOnClickListener {
                if (inscrito) {
                    db.child("grupos").child(grupoId).child("alumnos").child(uid).removeValue()
                } else {
                    db.child("grupos").child(grupoId).child("alumnos").child(uid).setValue(true)
                }
                cargarAlumnos()
            }
        }

        row.addView(tvNombre)
        row.addView(btn)
        layout.addView(row)
    }
}