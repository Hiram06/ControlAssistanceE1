package com.example.controlassistance

import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
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
        val etPassword  = findViewById<EditText>(R.id.etPassword)
        val btnLogin    = findViewById<Button>(R.id.btnLogin)

        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        tvRegister.visibility = View.GONE

        val opciones = arrayOf("Alumno", "Maestro", "Admin")

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, opciones) {

            // Vista del item SELECCIONADO (lo que se ve en el recuadro)
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = TextView(context)
                tv.text = opciones[position]
                tv.textSize = 16f
                tv.setTextColor(Color.BLACK)
                tv.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                tv.setPadding(16, 16, 16, 16)
                tv.gravity = android.view.Gravity.CENTER_VERTICAL
                return tv
            }

            // Vista del DROPDOWN (lista desplegable)
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = TextView(context)
                tv.text = opciones[position]
                tv.textSize = 16f
                tv.setTextColor(Color.parseColor("#333333"))
                tv.setBackgroundColor(Color.WHITE)
                tv.setPadding(32, 28, 32, 28)
                return tv
            }
        }

        spinnerTipo.adapter = adapter
        spinnerTipo.setPopupBackgroundResource(android.R.color.white)

        btnLogin.setOnClickListener {
            val matricula = etMatricula.text.toString().trim()
            val password  = etPassword.text.toString().trim()

            if (matricula.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val email = "$matricula@control.com"

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                        database.child("usuarios").child(uid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (!snapshot.exists()) {
                                        Toast.makeText(this@LoginActivity,
                                            "Usuario no encontrado. Contacta al administrador.",
                                            Toast.LENGTH_LONG).show()
                                        auth.signOut()
                                        return
                                    }
                                    val tipoReal = snapshot.child("tipo").getValue(String::class.java)?.trim()
                                    val tipoSeleccionado = spinnerTipo.selectedItem.toString()

                                    if (tipoReal != tipoSeleccionado) {
                                        Toast.makeText(this@LoginActivity,
                                            "Tipo de usuario incorrecto. Selecciona el tipo correcto.",
                                            Toast.LENGTH_LONG).show()
                                        auth.signOut()
                                        return
                                    }

                                    when (tipoReal) {
                                        "Admin"   -> startActivity(Intent(this@LoginActivity, AdminMainActivity::class.java))
                                        "Maestro" -> startActivity(Intent(this@LoginActivity, MaestroMainActivity::class.java))
                                        "Alumno"  -> startActivity(Intent(this@LoginActivity, AlumnoMainActivity::class.java))
                                        else -> {
                                            Toast.makeText(this@LoginActivity,
                                                "Tipo de usuario desconocido. Contacta al administrador.",
                                                Toast.LENGTH_LONG).show()
                                            auth.signOut()
                                        }
                                    }
                                    finish()
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })
                    } else {
                        Toast.makeText(this,
                            "Credenciales incorrectas. Contacta al administrador.",
                            Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}