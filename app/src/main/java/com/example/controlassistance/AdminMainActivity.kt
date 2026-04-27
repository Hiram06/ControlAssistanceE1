package com.example.controlassistance

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminMainActivity : AppCompatActivity() {

    private val auth   = FirebaseAuth.getInstance()
    private val db     = FirebaseDatabase.getInstance().reference
    private val grupos = mutableListOf<Grupo>()
    private lateinit var adapter: GrupoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerGruposAdmin)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = GrupoAdapter(
            grupos,
            onReporteClick = { grupo ->
                val intent = Intent(this, ReporteGrupoActivity::class.java)
                intent.putExtra("grupoId", grupo.id)
                intent.putExtra("grupoNombre", "${grupo.nombre} — ${grupo.materia}")
                startActivity(intent)
            },
            onAsistenciaClick = { grupo ->
                val intent = Intent(this, AdminInscribirAlumnosActivity::class.java)
                intent.putExtra("grupoId", grupo.id)
                intent.putExtra("grupoNombre", "${grupo.nombre} — ${grupo.materia}")
                startActivity(intent)
            },
            modoAdmin = true,
            onEliminarClick = { grupo ->
                AlertDialog.Builder(this)
                    .setTitle("Eliminar grupo")
                    .setMessage("¿Eliminar el grupo ${grupo.nombre}? Esta acción no se puede deshacer.")
                    .setPositiveButton("Eliminar") { _, _ ->
                        db.child("grupos").child(grupo.id).removeValue()
                        Toast.makeText(this, "Grupo eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btnCrearGrupoAdmin).setOnClickListener {
            startActivity(Intent(this, AdminCrearGrupoActivity::class.java))
        }

        findViewById<Button>(R.id.btnGestionarUsuarios).setOnClickListener {
            startActivity(Intent(this, AdminUsuariosActivity::class.java))
        }

        findViewById<Button>(R.id.btnCerrarSesionAdmin).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        cargarGrupos()
    }

    override fun onResume() {
        super.onResume()
        cargarGrupos()
    }

    private fun cargarGrupos() {
        db.child("grupos").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                grupos.clear()
                for (snap in snapshot.children) {
                    grupos.add(Grupo(
                        id        = snap.key ?: "",
                        nombre    = snap.child("nombre").getValue(String::class.java) ?: "",
                        materia   = snap.child("materia").getValue(String::class.java) ?: "",
                        maestroId = snap.child("maestroId").getValue(String::class.java) ?: ""
                    ))
                }
                adapter.notifyDataSetChanged()
                findViewById<TextView>(R.id.tvSinGruposAdmin).visibility =
                    if (grupos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}