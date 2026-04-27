package com.example.controlassistance

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminUsuariosActivity : AppCompatActivity() {

    private val db   = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private lateinit var llUsuarios: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_usuarios)

        llUsuarios = findViewById(R.id.llUsuarios)

        findViewById<Button>(R.id.btnVolverUsuarios).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAgregarUsuario).setOnClickListener { mostrarDialogoCrear() }

        cargarUsuarios()
    }

    private fun cargarUsuarios() {
        db.child("usuarios").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                llUsuarios.removeAllViews()
                for (snap in snapshot.children) {
                    val uid       = snap.key ?: continue
                    val nombre    = snap.child("nombre").getValue(String::class.java) ?: ""
                    val matricula = snap.child("matricula").getValue(String::class.java) ?: ""
                    val tipo      = snap.child("tipo").getValue(String::class.java) ?: ""

                    if (tipo == "Admin") continue // No mostrar al admin

                    val row = LinearLayout(this@AdminUsuariosActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(16, 12, 16, 12)
                        setBackgroundColor(android.graphics.Color.WHITE)
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.setMargins(0, 0, 0, 8) }
                        layoutParams = params
                    }

                    val tvInfo = TextView(this@AdminUsuariosActivity).apply {
                        text = "$nombre ($matricula) — $tipo"
                        textSize = 14f
                        layoutParams = LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val btnEliminar = Button(this@AdminUsuariosActivity).apply {
                        text = "Eliminar"
                        setBackgroundColor(android.graphics.Color.parseColor("#C0392B"))
                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 12f
                        setPadding(16, 8, 16, 8)
                        setOnClickListener {
                            AlertDialog.Builder(this@AdminUsuariosActivity)
                                .setTitle("Eliminar usuario")
                                .setMessage("¿Eliminar a $nombre?")
                                .setPositiveButton("Eliminar") { _, _ ->
                                    db.child("usuarios").child(uid).removeValue()
                                    Toast.makeText(this@AdminUsuariosActivity,
                                        "Usuario eliminado", Toast.LENGTH_SHORT).show()
                                }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        }
                    }

                    row.addView(tvInfo)
                    row.addView(btnEliminar)
                    llUsuarios.addView(row)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun mostrarDialogoCrear() {
        val view      = layoutInflater.inflate(R.layout.dialog_crear_usuario, null)
        val etNombre  = view.findViewById<EditText>(R.id.etNombreUsuario)
        val etMatric  = view.findViewById<EditText>(R.id.etMatriculaUsuario)
        val etPass    = view.findViewById<EditText>(R.id.etPasswordUsuario)
        val spinner   = view.findViewById<Spinner>(R.id.spinnerTipoUsuario)

        val tipos = listOf("Alumno", "Maestro")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tipos)

        AlertDialog.Builder(this)
            .setTitle("Crear usuario")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val nombre    = etNombre.text.toString().trim()
                val matricula = etMatric.text.toString().trim()
                val password  = etPass.text.toString().trim()
                val tipo      = spinner.selectedItem.toString()

                if (nombre.isEmpty() || matricula.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (password.length < 6) {
                    Toast.makeText(this, "Contraseña mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val email = "$matricula@control.com"
                val adminEmail    = auth.currentUser?.email ?: return@setPositiveButton
                val adminPassword = "admin123"

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener
                        val usuario = mapOf(
                            "nombre"    to nombre,
                            "matricula" to matricula,
                            "tipo"      to tipo
                        )
                        db.child("usuarios").child(uid).setValue(usuario)
                            .addOnSuccessListener {
                                // Volver a iniciar sesión como admin
                                auth.signInWithEmailAndPassword(adminEmail, adminPassword)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Usuario $nombre creado correctamente", Toast.LENGTH_LONG).show()
                                        cargarUsuarios()
                                    }
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}