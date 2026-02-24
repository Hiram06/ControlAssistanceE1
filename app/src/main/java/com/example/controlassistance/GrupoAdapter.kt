package com.example.controlassistance
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GrupoAdapter(
    private val listaGrupos: List<Grupo>
) : RecyclerView.Adapter<GrupoAdapter.GrupoViewHolder>() {

    class GrupoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombreGrupo: TextView = itemView.findViewById(R.id.tvNombreGrupo)
        val btnAsistencia: Button = itemView.findViewById(R.id.btnAsistencia)
        val btnReporte: Button = itemView.findViewById(R.id.btnReporte)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GrupoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grupo, parent, false)
        return GrupoViewHolder(view)
    }

    override fun onBindViewHolder(holder: GrupoViewHolder, position: Int) {
        val grupo = listaGrupos[position]

        holder.tvNombreGrupo.text = "${grupo.materia} - ${grupo.nombre}"

        holder.btnAsistencia.setOnClickListener {
            val intent = Intent(holder.itemView.context, TomarAsistenciaActivity::class.java)
            holder.itemView.context.startActivity(intent)
        }

        holder.btnReporte.setOnClickListener {
            val intent = Intent(holder.itemView.context, ReporteGrupoActivity::class.java)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return listaGrupos.size
    }
}