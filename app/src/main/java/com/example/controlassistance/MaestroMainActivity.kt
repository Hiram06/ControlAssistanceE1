package com.example.controlassistance

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MaestroMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.setBackgroundColor(android.graphics.Color.LTGRAY)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maestro_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerGrupos)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val listaGrupos = listOf(
            Grupo("001", "Grupo 1"),
            Grupo("002", "Grupo 2"),
            Grupo("003", "Grupo 3")
        )

        val adapter = GrupoAdapter(
            listaGrupos,
            onReporteClick = { grupoSeleccionado ->

                val intent = Intent(this, MaestroReporteActivity::class.java)
                intent.putExtra("grupo_nombre", grupoSeleccionado.nombre)
                startActivity(intent)
            },
            onAsistenciaClick = { grupoSeleccionado ->

                val intent = Intent(this, MaestroQRActivity::class.java)
                intent.putExtra("grupo_nombre", grupoSeleccionado.nombre)
                startActivity(intent)
            }
        )

        recyclerView.adapter = adapter
    }
}