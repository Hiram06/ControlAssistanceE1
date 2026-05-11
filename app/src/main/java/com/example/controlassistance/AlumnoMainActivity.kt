package com.example.controlassistance

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AlumnoMainActivity : AppCompatActivity() {

    private val auth   = FirebaseAuth.getInstance()
    private val db     = FirebaseDatabase.getInstance().reference
    private val grupos = mutableListOf<Grupo>()
    private lateinit var adapter: GrupoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alumno_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerGruposAlumno)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = GrupoAdapter(
            grupos,
            onReporteClick = { grupo ->
                val intent = Intent(this, AlumnoAsistenciaActivity::class.java)
                intent.putExtra("grupoId", grupo.id)
                intent.putExtra("grupoNombre", "${grupo.nombre} — ${grupo.materia}")
                startActivity(intent)
            },
            onAsistenciaClick = { _ -> },
            modoAlumno = true
        )
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btnEscanearQR).setOnClickListener {
            startActivity(Intent(this, AlumnoQRActivity::class.java))
        }

        findViewById<Button>(R.id.btnUnirseGrupo).setOnClickListener {
            mostrarDialogoUnirse()
        }

        findViewById<Button>(R.id.btnCerrarSesionAlumno).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        cargarGrupos()
        verificarSolicitudes()
    }

    private fun cargarGrupos() {
        val uid = auth.currentUser?.uid ?: return
        db.child("grupos").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                grupos.clear()
                for (snap in snapshot.children) {
                    val tieneAlumno = snap.child("alumnos").child(uid).exists()
                    if (tieneAlumno) {
                        grupos.add(Grupo(
                            id        = snap.key ?: "",
                            nombre    = snap.child("nombre").getValue(String::class.java) ?: "",
                            materia   = snap.child("materia").getValue(String::class.java) ?: "",
                            maestroId = snap.child("maestroId").getValue(String::class.java) ?: ""
                        ))
                    }
                }
                adapter.notifyDataSetChanged()
                val tvVacio = findViewById<TextView>(R.id.tvSinGruposAlumno)
                tvVacio.visibility = if (grupos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun verificarSolicitudes() {
        val uid = auth.currentUser?.uid ?: return
        db.child("solicitudes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tvEstado = findViewById<TextView>(R.id.tvEstadoSolicitud)
                var pendiente = false
                var rechazada = false

                for (grupoSnap in snapshot.children) {
                    val miSolicitud = grupoSnap.child(uid)
                    if (miSolicitud.exists()) {
                        val estado = miSolicitud.child("estado").getValue(String::class.java) ?: ""
                        when (estado) {
                            "pendiente" -> pendiente = true
                            "rechazada" -> rechazada = true
                        }
                    }
                }

                when {
                    rechazada -> {
                        tvEstado.visibility = android.view.View.VISIBLE
                        tvEstado.text = "❌ Una de tus solicitudes fue rechazada."
                        tvEstado.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
                    }
                    pendiente -> {
                        tvEstado.visibility = android.view.View.VISIBLE
                        tvEstado.text = "⏳ Tienes solicitudes pendientes de aprobación."
                        tvEstado.setTextColor(android.graphics.Color.parseColor("#E67E22"))
                    }
                    else -> tvEstado.visibility = android.view.View.GONE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun mostrarDialogoUnirse() {
        val uid = auth.currentUser?.uid ?: return
        val input = EditText(this).apply {
            hint = "ID del grupo"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Solicitar unirse a un grupo")
            .setMessage("Ingresa el ID del grupo que te dio tu maestro")
            .setView(input)
            .setPositiveButton("Enviar solicitud") { _, _ ->
                val grupoId = input.text.toString().trim()
                if (grupoId.isEmpty()) {
                    Toast.makeText(this, "Escribe el ID del grupo", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                db.child("grupos").child(grupoId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                Toast.makeText(this@AlumnoMainActivity,
                                    "Grupo no encontrado", Toast.LENGTH_SHORT).show()
                                return
                            }
                            // Verificar si ya está inscrito
                            if (snapshot.child("alumnos").child(uid).exists()) {
                                Toast.makeText(this@AlumnoMainActivity,
                                    "Ya estás inscrito en este grupo", Toast.LENGTH_SHORT).show()
                                return
                            }
                            // Verificar si ya tiene solicitud pendiente
                            db.child("solicitudes").child(grupoId).child(uid)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(solSnap: DataSnapshot) {
                                        if (solSnap.exists()) {
                                            val estado = solSnap.child("estado").getValue(String::class.java) ?: ""
                                            if (estado == "pendiente") {
                                                Toast.makeText(this@AlumnoMainActivity,
                                                    "Ya tienes una solicitud pendiente en este grupo",
                                                    Toast.LENGTH_SHORT).show()
                                                return
                                            }
                                        }
                                        // Obtener nombre del alumno
                                        db.child("usuarios").child(uid).child("nombre")
                                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(nombreSnap: DataSnapshot) {
                                                    val nombre = nombreSnap.getValue(String::class.java) ?: ""
                                                    val solicitud = mapOf(
                                                        "alumnoId"  to uid,
                                                        "nombre"    to nombre,
                                                        "estado"    to "pendiente"
                                                    )
                                                    db.child("solicitudes").child(grupoId).child(uid)
                                                        .setValue(solicitud)
                                                        .addOnSuccessListener {
                                                            Toast.makeText(this@AlumnoMainActivity,
                                                                "Solicitud enviada. Espera la aprobación del maestro.",
                                                                Toast.LENGTH_LONG).show()
                                                        }
                                                }
                                                override fun onCancelled(e: DatabaseError) {}
                                            })
                                    }
                                    override fun onCancelled(e: DatabaseError) {}
                                })
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}