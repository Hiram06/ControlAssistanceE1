package com.example.controlassistance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AlumnoAsistenciaActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alumno_asistencia)

        findViewById<Button>(R.id.btnVolver).setOnClickListener { finish() }

        val uid      = auth.currentUser?.uid ?: return
        val grupoId  = intent.getStringExtra("grupoId") ?: ""
        val grupoNombre = intent.getStringExtra("grupoNombre") ?: ""

        findViewById<TextView>(R.id.tvTituloAsistencia).text = grupoNombre

        if (grupoId.isEmpty()) {
            updateUI(0, 0, uid, grupoId)
        } else {
            cargarAsistencias(uid, grupoId)
        }
    }

    private fun cargarAsistencias(uid: String, grupoId: String) {
        db.child("asistencias").child(grupoId).child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var presentes = 0
                    var faltas    = 0
                    for (dia in snapshot.children) {
                        val presente = dia.child("presente").getValue(Boolean::class.java) ?: false
                        if (presente) presentes++ else faltas++
                    }
                    updateUI(presentes, faltas, uid, grupoId)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateUI(presentes: Int, faltas: Int, uid: String, grupoId: String) {
        val total = presentes + faltas
        val pct   = if (total > 0) presentes * 100 / total else 0

        val tvPorcentaje = findViewById<TextView>(R.id.tvPorcentaje)
        tvPorcentaje.text = "Porcentaje de asistencia: $pct%\n$presentes presente(s) de $total clase(s)"

        val tvFaltas = findViewById<TextView>(R.id.tvFaltas)
        if (faltas >= 2) {
            tvFaltas.text = "Tienes $faltas faltas acumuladas. ¡Cuidado!"
            tvFaltas.setTextColor(Color.parseColor("#8B0000"))
        } else {
            tvFaltas.text = "Tienes $faltas falta(s). ¡Sigue así!"
            tvFaltas.setTextColor(Color.parseColor("#1B5E20"))
        }

        findViewById<Button>(R.id.btnGenerarPdf).setOnClickListener {
            val intent = Intent(this, GenerarPdfAlumnoActivity::class.java)
            intent.putExtra("uid", uid)
            intent.putExtra("grupoId", grupoId)
            startActivity(intent)
        }
    }
}