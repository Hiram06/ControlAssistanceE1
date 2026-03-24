package com.example.controlassistance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AlumnoMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alumno_main)

        findViewById<Button>(R.id.btnEscanearQR).setOnClickListener {
            startActivity(Intent(this, AlumnoQRActivity::class.java))
        }

        findViewById<Button>(R.id.btnReporte).setOnClickListener {
            startActivity(Intent(this, AlumnoAsistenciaActivity::class.java))
        }
    }
}