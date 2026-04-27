package com.example.controlassistance

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class AdminCrearGrupoActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance().reference
    private val maestros = mutableListOf<Pair<String, String>>() // uid, nombre

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_crear_grupo)

        val etNombre   = findViewById<EditText>(R.id.etNombreGrupoAdmin)
        val etMateria  = findViewById<EditText>(R.id.etMateriaGrupoAdmin)
        val spinner    = findViewById<Spinner>(R.id.spinnerMaestro)
        val btnCrear   = findViewById<Button>(R.id.btnCrearGrupoConfirmar)
        val btnVolver  = findViewById<Button>(R.id.btnVolverAdmin)

        btnVolver.setOnClickListener { finish() }

        // Cargar maestros
        db.child("usuarios").orderByChild("tipo").equalTo("Maestro")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    maestros.clear()
                    for (snap in snapshot.children) {
                        val uid    = snap.key ?: continue
                        val nombre = snap.child("nombre").getValue(String::class.java) ?: continue
                        maestros.add(Pair(uid, nombre))
                    }
                    val nombres = maestros.map { it.second }
                    spinner.adapter = ArrayAdapter(this@AdminCrearGrupoActivity,
                        android.R.layout.simple_spinner_dropdown_item, nombres)
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        btnCrear.setOnClickListener {
            val nombre  = etNombre.text.toString().trim()
            val materia = etMateria.text.toString().trim()

            if (nombre.isEmpty() || materia.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (maestros.isEmpty()) {
                Toast.makeText(this, "No hay maestros registrados", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val maestroSeleccionado = maestros[spinner.selectedItemPosition]
            val grupoRef = db.child("grupos").push()
            val grupoId  = grupoRef.key ?: return@setOnClickListener

            val grupo = mapOf(
                "nombre"    to nombre,
                "materia"   to materia,
                "maestroId" to maestroSeleccionado.first
            )

            grupoRef.setValue(grupo).addOnSuccessListener {
                Toast.makeText(this, "Grupo creado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}