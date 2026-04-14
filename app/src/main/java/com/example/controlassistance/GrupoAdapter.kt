package com.example.controlassistance

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class GrupoAdapter(
    private val lista: MutableList<Grupo>,
    private val onReporteClick: (Grupo) -> Unit,
    private val onAsistenciaClick: (Grupo) -> Unit,
    private val modoAlumno: Boolean = false
) : RecyclerView.Adapter<GrupoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreGrupo)
        val tvIdGrupo: TextView = view.findViewById(R.id.tvIdGrupo)
        val btnReporte: Button = view.findViewById(R.id.btnReporte)
        val btnAsistencia: Button = view.findViewById(R.id.btnAsistencia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_grupo, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val grupo = lista[position]
        holder.tvNombre.text = "${grupo.nombre} — ${grupo.materia}"

        if (modoAlumno) {
            // En modo alumno: solo mostrar botón de ver asistencia, ocultar el de tomar asistencia
            holder.tvIdGrupo.visibility = View.GONE
            holder.btnAsistencia.visibility = View.GONE
            holder.btnReporte.text = "Ver mi asistencia"
            holder.btnReporte.setOnClickListener { onReporteClick(grupo) }
        } else {
            // En modo maestro: mostrar ID copiable y ambos botones
            holder.tvIdGrupo.visibility = View.VISIBLE
            holder.btnAsistencia.visibility = View.VISIBLE
            holder.tvIdGrupo.text = "ID: ${grupo.id}  (toca para copiar)"
            holder.tvIdGrupo.setOnClickListener {
                val clipboard = holder.itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("ID Grupo", grupo.id)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(holder.itemView.context, "ID copiado al portapapeles", Toast.LENGTH_SHORT).show()
            }
            holder.btnReporte.setOnClickListener { onReporteClick(grupo) }
            holder.btnAsistencia.setOnClickListener { onAsistenciaClick(grupo) }
        }
    }
}