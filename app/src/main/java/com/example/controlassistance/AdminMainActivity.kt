package com.example.controlassistance

import android.content.Intent
import android.os.Bundle
import android.view.View
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
    private lateinit var btnEliminar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerGruposAdmin)
        recyclerView.layoutManager = LinearLayoutManager(this)
        btnEliminar = findViewById(R.id.btnEliminarGruposAdmin)

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
            // Cuando cambia la selección → mostrar/ocultar botón eliminar
            onSeleccionCambia = { cantidad ->
                if (cantidad > 0) {
                    btnEliminar.text = "Eliminar $cantidad grupo(s) seleccionado(s)"
                    btnEliminar.visibility = View.VISIBLE
                } else {
                    btnEliminar.visibility = View.GONE
                }
            }
        )
        recyclerView.adapter = adapter

        // Botón eliminar con confirmación
        btnEliminar.setOnClickListener {
            val seleccionados = adapter.gruposSeleccionados.toSet()
            if (seleccionados.isEmpty()) return@setOnClickListener

            AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Seguro que quieres eliminar ${seleccionados.size} grupo(s)? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar") { _, _ ->
                    for (grupoId in seleccionados) {
                        db.child("grupos").child(grupoId).removeValue()
                    }
                    adapter.gruposSeleccionados.clear()
                    btnEliminar.visibility = View.GONE
                    Toast.makeText(this,
                        "${seleccionados.size} grupo(s) eliminado(s)",
                        Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

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
                    if (grupos.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}