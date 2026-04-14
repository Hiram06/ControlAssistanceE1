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

        cargarGrupos()
    }

    private fun cargarGrupos() {
        val uid = auth.currentUser?.uid ?: return
        db.child("grupos")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    grupos.clear()
                    for (snap in snapshot.children) {
                        val tieneAlumno = snap.child("alumnos").child(uid).exists()
                        if (tieneAlumno) {
                            grupos.add(
                                Grupo(
                                    id        = snap.key ?: "",
                                    nombre    = snap.child("nombre").getValue(String::class.java) ?: "",
                                    materia   = snap.child("materia").getValue(String::class.java) ?: "",
                                    maestroId = snap.child("maestroId").getValue(String::class.java) ?: ""
                                )
                            )
                        }
                    }
                    adapter.notifyDataSetChanged()
                    val tvVacio = findViewById<TextView>(R.id.tvSinGruposAlumno)
                    tvVacio.visibility = if (grupos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
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
            .setTitle("Unirse a un grupo")
            .setMessage("Pide el ID del grupo a tu maestro")
            .setView(input)
            .setPositiveButton("Unirse") { _, _ ->
                val grupoId = input.text.toString().trim()
                if (grupoId.isEmpty()) {
                    Toast.makeText(this, "Escribe el ID del grupo", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                db.child("grupos").child(grupoId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                Toast.makeText(this@AlumnoMainActivity, "Grupo no encontrado", Toast.LENGTH_SHORT).show()
                                return
                            }
                            db.child("grupos").child(grupoId).child("alumnos").child(uid).setValue(true)
                                .addOnSuccessListener {
                                    Toast.makeText(this@AlumnoMainActivity, "Te uniste al grupo exitosamente", Toast.LENGTH_SHORT).show()
                                }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}