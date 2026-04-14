package com.example.controlassistance

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val etNombre     = findViewById<EditText>(R.id.etNombre)
        val etMatricula  = findViewById<EditText>(R.id.etMatriculaRegister)
        val etPassword   = findViewById<EditText>(R.id.etPasswordRegister)
        val spinner      = findViewById<Spinner>(R.id.spinnerTipoRegister)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        val opciones = listOf("Alumno", "Maestro")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, opciones)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinner.adapter = adapter

        btnRegistrar.setOnClickListener {
            val nombre    = etNombre.text.toString().trim()
            val matricula = etMatricula.text.toString().trim()
            val password  = etPassword.text.toString().trim()
            val tipo      = spinner.selectedItem.toString()

            if (nombre.isEmpty() || matricula.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Contraseña mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val email = "$matricula@control.com"

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser!!.uid
                        val usuario = mapOf(
                            "nombre"    to nombre,
                            "matricula" to matricula,
                            "tipo"      to tipo
                        )
                        database.child("usuarios").child(uid).setValue(usuario)

                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()

                        if (tipo == "Alumno") {
                            startActivity(Intent(this, AlumnoMainActivity::class.java))
                        } else {
                            startActivity(Intent(this, MaestroMainActivity::class.java))
                        }
                        finish()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}