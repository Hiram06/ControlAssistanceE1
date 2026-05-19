package com.example.controlassistance

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GrupoAdapter(
    private val lista: MutableList<Grupo>,
    private val onReporteClick: (Grupo) -> Unit,
    private val onAsistenciaClick: (Grupo) -> Unit,
    private val onListaClick: ((Grupo) -> Unit)? = null,
    private val modoAlumno: Boolean = false,
    private val modoAdmin: Boolean = false,
    private val onEliminarClick: ((Grupo) -> Unit)? = null,
    private val onSeleccionCambia: ((Int) -> Unit)? = null,
    private val onInscripcionClick: ((Grupo) -> Unit)? = null
) : RecyclerView.Adapter<GrupoAdapter.ViewHolder>() {

    private val db = FirebaseDatabase.getInstance().reference

    // Set con los IDs de grupos seleccionados para eliminar
    val gruposSeleccionados = mutableSetOf<String>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView        = view.findViewById(R.id.tvNombreGrupo)
        val tvIdGrupo: TextView       = view.findViewById(R.id.tvIdGrupo)
        val llBotones: LinearLayout   = view.findViewById(R.id.llBotones)
        val btnReporte: Button        = view.findViewById(R.id.btnReporte)
        val btnAsistencia: Button     = view.findViewById(R.id.btnAsistencia)
        val btnLista: Button          = view.findViewById(R.id.btnLista)
        val btnEliminarGrupo: Button  = view.findViewById(R.id.btnEliminarGrupo)
        val btnInscripcionQR: Button  = view.findViewById(R.id.btnInscripcionQR)
        val tvPunto: TextView         = view.findViewById(R.id.tvPuntoRojo)
        val checkbox: CheckBox        = view.findViewById(R.id.checkboxGrupo)
        val llDetalles: LinearLayout  = view.findViewById(R.id.llDetallesGrupo)
        val tvDetalleMaestro: TextView = view.findViewById(R.id.tvDetalleMaestro)
        val tvDetalleAlumnos: TextView = view.findViewById(R.id.tvDetalleAlumnos)
        val tvDetalleId: TextView     = view.findViewById(R.id.tvDetalleId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grupo, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val grupo = lista[position]
        holder.tvNombre.text = "${grupo.nombre} — ${grupo.materia}"

        // Siempre ocultar el botón eliminar individual (ahora es global al fondo)
        holder.btnEliminarGrupo.visibility = View.GONE

        when {
            modoAlumno -> {
                holder.llBotones.visibility    = View.VISIBLE
                holder.tvIdGrupo.visibility    = View.GONE
                holder.btnAsistencia.visibility = View.GONE
                holder.btnLista.visibility     = View.GONE
                holder.tvPunto.visibility      = View.GONE
                holder.checkbox.visibility     = View.GONE
                holder.llDetalles.visibility   = View.GONE
                holder.btnInscripcionQR.visibility = View.GONE
                holder.btnReporte.text = "Ver mi asistencia"
                holder.btnReporte.setOnClickListener { onReporteClick(grupo) }
            }

            modoAdmin -> {
                holder.llBotones.visibility    = View.GONE
                holder.tvIdGrupo.visibility    = View.GONE
                holder.tvPunto.visibility      = View.GONE
                holder.checkbox.visibility     = View.VISIBLE
                holder.btnInscripcionQR.visibility = View.GONE

                // Restaurar estado del checkbox si ya estaba seleccionado
                holder.checkbox.setOnCheckedChangeListener(null)
                holder.checkbox.isChecked = gruposSeleccionados.contains(grupo.id)
                holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) gruposSeleccionados.add(grupo.id)
                    else gruposSeleccionados.remove(grupo.id)
                    onSeleccionCambia?.invoke(gruposSeleccionados.size)
                }

                // Click en la tarjeta → mostrar/ocultar detalles
                holder.itemView.setOnClickListener {
                    val visible = holder.llDetalles.visibility == View.VISIBLE
                    if (visible) {
                        holder.llDetalles.visibility = View.GONE
                    } else {
                        // Cargar nombre del maestro desde Firebase
                        holder.tvDetalleId.text = "ID: ${grupo.id}"
                        val numAlumnos = 0 // se actualizará con Firebase abajo
                        holder.tvDetalleAlumnos.text = "Alumnos inscritos: cargando..."
                        holder.tvDetalleMaestro.text = "Maestro: cargando..."

                        // Obtener nombre del maestro
                        db.child("usuarios").child(grupo.maestroId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snap: DataSnapshot) {
                                    val nombre = snap.child("nombre")
                                        .getValue(String::class.java) ?: "Desconocido"
                                    holder.tvDetalleMaestro.text = "Maestro: $nombre"
                                }
                                override fun onCancelled(e: DatabaseError) {}
                            })

                        // Contar alumnos inscritos
                        db.child("grupos").child(grupo.id).child("alumnos")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snap: DataSnapshot) {
                                    val count = snap.childrenCount
                                    holder.tvDetalleAlumnos.text = "Alumnos inscritos: $count"
                                }
                                override fun onCancelled(e: DatabaseError) {}
                            })

                        holder.llDetalles.visibility = View.VISIBLE
                    }
                }
            }

            else -> {
                holder.llBotones.visibility    = View.VISIBLE
                holder.tvIdGrupo.visibility    = View.VISIBLE
                holder.btnAsistencia.visibility = View.VISIBLE
                holder.btnLista.visibility     = View.VISIBLE
                holder.checkbox.visibility     = View.GONE
                holder.llDetalles.visibility   = View.GONE
                holder.btnInscripcionQR.visibility = View.VISIBLE
                holder.tvIdGrupo.text = "ID: ${grupo.id}  (toca para copiar)"
                holder.tvIdGrupo.setTextColor(Color.BLACK)
                holder.tvIdGrupo.setOnClickListener {
                    val clipboard = holder.itemView.context
                        .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("ID Grupo", grupo.id)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(holder.itemView.context,
                        "ID copiado al portapapeles", Toast.LENGTH_SHORT).show()
                }
                holder.btnLista.setOnClickListener { onListaClick?.invoke(grupo) }
                holder.btnReporte.setOnClickListener { onReporteClick(grupo) }
                holder.btnAsistencia.setOnClickListener { onAsistenciaClick(grupo) }
                holder.btnInscripcionQR.setOnClickListener { onInscripcionClick?.invoke(grupo) }

                // Escuchar solicitudes pendientes
                db.child("solicitudes").child(grupo.id)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val pendientes = snapshot.children.count {
                                it.child("estado").getValue(String::class.java) == "pendiente"
                            }
                            if (pendientes > 0) {
                                holder.tvPunto.visibility = View.VISIBLE
                                holder.tvPunto.text = "● $pendientes solicitud(es) pendiente(s)"
                            } else {
                                holder.tvPunto.visibility = View.GONE
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }
    }
}