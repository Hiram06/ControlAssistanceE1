package com.example.controlassistance

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MaestroMainActivity : AppCompatActivity() {

    private val auth   = FirebaseAuth.getInstance()
    private val db     = FirebaseDatabase.getInstance().reference
    private val grupos = mutableListOf<Grupo>()
    private lateinit var adapter: GrupoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maestro_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerGrupos)
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
                val intent = Intent(this, TomarAsistenciaActivity::class.java)
                intent.putExtra("grupoId", grupo.id)
                intent.putExtra("grupoNombre", "${grupo.nombre} — ${grupo.materia}")
                startActivity(intent)
            },
            onListaClick = { grupo ->
                val intent = Intent(this, ListaAlumnosActivity::class.java)
                intent.putExtra("grupoId", grupo.id)
                intent.putExtra("grupoNombre", "${grupo.nombre} — ${grupo.materia}")
                startActivity(intent)
            }
        )
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btnCerrarSesionMaestro).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        cargarGrupos()
    }

    private fun cargarGrupos() {
        val uid = auth.currentUser?.uid ?: return
        db.child("grupos").orderByChild("maestroId").equalTo(uid)
            .addValueEventListener(object : ValueEventListener {
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
                    val tvVacio = findViewById<TextView>(R.id.tvSinGrupos)
                    tvVacio.visibility = if (grupos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}