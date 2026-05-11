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
                    if (tipo == "Admin") continue

                    val row = LinearLayout(this@AdminUsuariosActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(16, 12, 16, 12)
                        setBackgroundColor(android.graphics.Color.WHITE)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.setMargins(0, 0, 0, 8) }
                    }

                    val tvInfo = TextView(this@AdminUsuariosActivity).apply {
                        text = "$nombre ($matricula) — $tipo"
                        textSize = 14f
                        setTextColor(android.graphics.Color.BLACK)
                        layoutParams = LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val btnPromover = Button(this@AdminUsuariosActivity).apply {
                        text = "Admin"
                        setBackgroundColor(android.graphics.Color.parseColor("#E67E22"))
                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 11f
                        setPadding(12, 8, 12, 8)
                        setOnClickListener {
                            AlertDialog.Builder(this@AdminUsuariosActivity)
                                .setTitle("Promover a Admin")
                                .setMessage("¿Promover a $nombre como administrador?")
                                .setPositiveButton("Promover") { _, _ ->
                                    db.child("usuarios").child(uid).child("tipo").setValue("Admin")
                                    Toast.makeText(this@AdminUsuariosActivity,
                                        "$nombre ahora es Admin", Toast.LENGTH_SHORT).show()
                                }
                                .setNegativeButton("Cancelar", null).show()
                        }
                    }

                    val btnEliminar = Button(this@AdminUsuariosActivity).apply {
                        text = "Eliminar"
                        setBackgroundColor(android.graphics.Color.parseColor("#C0392B"))
                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 11f
                        setPadding(12, 8, 12, 8)
                        setOnClickListener {
                            AlertDialog.Builder(this@AdminUsuariosActivity)
                                .setTitle("Eliminar usuario")
                                .setMessage("¿Eliminar a $nombre?")
                                .setPositiveButton("Eliminar") { _, _ ->
                                    db.child("usuarios").child(uid).removeValue()
                                    Toast.makeText(this@AdminUsuariosActivity,
                                        "Usuario eliminado", Toast.LENGTH_SHORT).show()
                                }
                                .setNegativeButton("Cancelar", null).show()
                        }
                    }

                    row.addView(tvInfo)
                    row.addView(btnPromover)
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
        val etPass    = view.findViewById<EditText>(R.id.etPasswordUsuario)
        val spinner   = view.findViewById<Spinner>(R.id.spinnerTipoUsuario)
        val etMatric  = view.findViewById<EditText>(R.id.etMatriculaUsuario)
        etMatric.visibility = android.view.View.GONE // matrícula es automática

        val tipos = listOf("Alumno", "Maestro")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tipos)

        AlertDialog.Builder(this)
            .setTitle("Crear usuario")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val nombre   = etNombre.text.toString().trim()
                val password = etPass.text.toString().trim()
                val tipo     = spinner.selectedItem.toString()

                if (nombre.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (password.length < 6) {
                    Toast.makeText(this, "Contraseña mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                generarMatriculaYCrear(nombre, password, tipo)
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun generarMatriculaYCrear(nombre: String, password: String, tipo: String) {
        db.child("usuarios").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var maxMaestro = -1
                var maxAlumno  = 99999

                for (snap in snapshot.children) {
                    val t = snap.child("tipo").getValue(String::class.java) ?: ""
                    val m = snap.child("matricula").getValue(String::class.java) ?: ""
                    val num = m.toIntOrNull() ?: continue
                    if (t == "Maestro" && num <= 99999 && num > maxMaestro) maxMaestro = num
                    if (t == "Alumno"  && num >= 100000 && num > maxAlumno) maxAlumno = num
                }

                val nuevaMatricula = if (tipo == "Maestro") {
                    String.format("%06d", maxMaestro + 1)
                } else {
                    String.format("%06d", maxAlumno + 1)
                }

                val email = "$nuevaMatricula@control.com"
                val adminEmail    = auth.currentUser?.email ?: return
                val adminPassword = "admin123"

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener
                        val usuario = mapOf(
                            "nombre"    to nombre,
                            "matricula" to nuevaMatricula,
                            "tipo"      to tipo
                        )
                        db.child("usuarios").child(uid).setValue(usuario)
                            .addOnSuccessListener {
                                auth.signInWithEmailAndPassword(adminEmail, adminPassword)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@AdminUsuariosActivity,
                                            "Usuario creado. Matrícula: $nuevaMatricula",
                                            Toast.LENGTH_LONG).show()
                                        cargarUsuarios()
                                    }
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@AdminUsuariosActivity,
                            "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        auth.signInWithEmailAndPassword(adminEmail, adminPassword)
                    }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}