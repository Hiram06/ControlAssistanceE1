package com.example.controlassistance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GrupoAdapter(
    private val lista: List<Grupo>,
    private val onReporteClick: (Grupo) -> Unit,
    private val onAsistenciaClick: (Grupo) -> Unit
) : RecyclerView.Adapter<GrupoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreGrupo)
        val btnReporte: Button = view.findViewById(R.id.btnReporte)
        val btnAsistencia: Button = view.findViewById(R.id.btnAsistencia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grupo, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val grupo = lista[position]

        holder.tvNombre.text = grupo.nombre

        holder.btnReporte.setOnClickListener {
            onReporteClick(grupo)
        }

        holder.btnAsistencia.setOnClickListener {
            onAsistenciaClick(grupo)
        }
    }
}