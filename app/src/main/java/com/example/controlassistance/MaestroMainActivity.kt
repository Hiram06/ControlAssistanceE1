package com.example.controlassistance

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
            Grupo("001", "Cálculo"),
            Grupo("002", "Física"),
            Grupo("003", "Programación")
        )

        val adapter = GrupoAdapter(listaGrupos)
        recyclerView.adapter = adapter
    }
}