package com.example.controlassistance

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AlumnoAsistenciaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alumno_asistencia)

        findViewById<Button>(R.id.btnVolver).setOnClickListener {
            finish()
        }
    }
}