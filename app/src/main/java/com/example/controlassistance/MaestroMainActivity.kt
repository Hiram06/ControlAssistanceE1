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
            }
        )
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btnCrearGrupo).setOnClickListener {
            mostrarDialogoCrearGrupo()
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
                        grupos.add(
                            Grupo(
                                id        = snap.key ?: "",
                                nombre    = snap.child("nombre").getValue(String::class.java) ?: "",
                                materia   = snap.child("materia").getValue(String::class.java) ?: "",
                                maestroId = snap.child("maestroId").getValue(String::class.java) ?: ""
                            )
                        )
                    }
                    adapter.notifyDataSetChanged()
                    val tvVacio = findViewById<TextView>(R.id.tvSinGrupos)
                    tvVacio.visibility = if (grupos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MaestroMainActivity, "Error cargando grupos", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun mostrarDialogoCrearGrupo() {
        val uid = auth.currentUser?.uid ?: return
        val view = layoutInflater.inflate(R.layout.dialog_crear_grupo, null)
        AlertDialog.Builder(this)
            .setTitle("Crear nuevo grupo")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val nombre  = view.findViewById<EditText>(R.id.etNombreGrupo).text.toString().trim()
                val materia = view.findViewById<EditText>(R.id.etMateriaGrupo).text.toString().trim()
                if (nombre.isEmpty() || materia.isEmpty()) {
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val grupoRef = db.child("grupos").push()
                val grupoId  = grupoRef.key ?: return@setPositiveButton
                val grupo = mapOf(
                    "nombre"    to nombre,
                    "materia"   to materia,
                    "maestroId" to uid
                )
                grupoRef.setValue(grupo).addOnSuccessListener {
                    AlertDialog.Builder(this)
                        .setTitle("Grupo creado exitosamente")
                        .setMessage("Comparte este ID con tus alumnos para que puedan unirse:\n\n$grupoId")
                        .setPositiveButton("Entendido", null)
                        .setCancelable(false)
                        .show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}