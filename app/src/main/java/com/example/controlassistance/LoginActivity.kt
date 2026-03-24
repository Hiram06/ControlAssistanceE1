package com.example.controlassistance

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val spinnerTipo = findViewById<Spinner>(R.id.spinnerTipo)
        val etMatricula = findViewById<EditText>(R.id.etMatricula)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        val opciones = arrayOf("Alumno", "Maestro")

        val adapter = object : ArrayAdapter<String>(this, R.layout.spinner_item, opciones) {
            override fun getDropDownView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(android.graphics.Color.parseColor("#555555"))
                view.setBackgroundColor(android.graphics.Color.WHITE)
                view.textSize = 16f
                view.setPadding(32, 24, 32, 24)
                return view
            }
        }

        spinnerTipo.adapter = adapter
        spinnerTipo.setPopupBackgroundResource(android.R.color.white)

        btnLogin.setOnClickListener {
            val matricula = etMatricula.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (matricula.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val email = "$matricula@control.com"

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        if (uid == null) {
                            Toast.makeText(this, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
                            return@addOnCompleteListener
                        }

                        database.child("usuarios").child(uid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (!snapshot.exists()) {
                                        Toast.makeText(
                                            this@LoginActivity,
                                            "Usuario no registrado en DB",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return
                                    }

                                    val tipo = snapshot.child("tipo").getValue(String::class.java)?.trim()

                                    when (tipo) {
                                        "Alumno" -> startActivity(Intent(this@LoginActivity, AlumnoMainActivity::class.java))
                                        "Maestro" -> startActivity(Intent(this@LoginActivity, MaestroMainActivity::class.java))
                                        else -> Toast.makeText(
                                            this@LoginActivity,
                                            "Tipo de usuario desconocido",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    finish()
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Error al obtener datos: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })

                    } else {
                        Toast.makeText(
                            this,
                            "Credenciales incorrectas: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}