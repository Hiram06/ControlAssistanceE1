package com.example.controlassistance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MaestroReporteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maestro_reporte)

        val nombreGrupo = intent.getStringExtra("grupo_nombre")

        val tvTitulo = findViewById<TextView>(R.id.tvTituloGrupo)
        val btnIndividual = findViewById<Button>(R.id.btnIndividual)

        tvTitulo.text = "Reporte de $nombreGrupo"

        btnIndividual.setOnClickListener {
            val intent = Intent(this, MaestroReporteIndividualActivity::class.java)
            intent.putExtra("grupo_nombre", nombreGrupo)
            startActivity(intent)
        }
    }
}