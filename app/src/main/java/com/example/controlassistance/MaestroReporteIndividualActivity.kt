package com.example.controlassistance

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MaestroReporteIndividualActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maestro_reporte_individual)

        val nombreGrupo = intent.getStringExtra("grupo_nombre")

        val tvTitulo = findViewById<TextView>(R.id.tvTituloIndividual)

        tvTitulo.text = "Reporte individual de $nombreGrupo"
    }
}