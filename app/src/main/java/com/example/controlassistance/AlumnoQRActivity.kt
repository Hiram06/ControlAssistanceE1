package com.example.controlassistance

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AlumnoQRActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alumno_qr)

        findViewById<Button>(R.id.btnVolver).setOnClickListener {
            finish()
        }
    }
}