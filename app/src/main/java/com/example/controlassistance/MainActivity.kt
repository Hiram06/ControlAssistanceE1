package com.example.controlassistance

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        FirebaseDatabase.getInstance().reference
            .child("usuarios").child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    when (snapshot.child("tipo").getValue(String::class.java)?.trim()) {
                        "Admin"   -> startActivity(Intent(this@MainActivity, AdminMainActivity::class.java))
                        "Maestro" -> startActivity(Intent(this@MainActivity, MaestroMainActivity::class.java))
                        "Alumno"  -> startActivity(Intent(this@MainActivity, AlumnoMainActivity::class.java))
                        else      -> startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    }
                    finish()
                }
                override fun onCancelled(error: DatabaseError) {
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            })
    }
}