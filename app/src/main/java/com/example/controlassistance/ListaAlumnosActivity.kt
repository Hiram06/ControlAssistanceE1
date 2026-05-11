package com.example.controlassistance

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ListaAlumnosActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance().reference
    private lateinit var grupoId: String
    private lateinit var grupoNombre: String
    private lateinit var tableLayout: TableLayout
    private val filas = mutableListOf<RowAlumno>()
    private val todasFechas = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_alumnos)

        grupoId     = intent.getStringExtra("grupoId") ?: ""
        grupoNombre = intent.getStringExtra("grupoNombre") ?: ""

        tableLayout = findViewById(R.id.tableListaAlumnos)
        findViewById<TextView>(R.id.tvTituloLista).text = grupoNombre
        findViewById<Button>(R.id.btnVolverLista).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnVerSolicitudes).setOnClickListener {
            val intent = Intent(this, SolicitudesActivity::class.java)
            intent.putExtra("grupoId", grupoId)
            intent.putExtra("grupoNombre", grupoNombre)
            startActivity(intent)
        }

        cargarDatos()
        escucharSolicitudes()
    }

    private fun escucharSolicitudes() {
        db.child("solicitudes").child(grupoId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pendientes = snapshot.children.count {
                        it.child("estado").getValue(String::class.java) == "pendiente"
                    }
                    val btn = findViewById<Button>(R.id.btnVerSolicitudes)
                    if (pendientes > 0) {
                        btn.text = "● Solicitudes ($pendientes)"
                        btn.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#E74C3C"))
                    } else {
                        btn.text = "Solicitudes"
                        btn.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#4F6BED"))
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun cargarDatos() {
        db.child("grupos").child(grupoId).child("alumnos")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    filas.clear(); todasFechas.clear()
                    val uidList = snapshot.children.mapNotNull { it.key }.filter { it.isNotEmpty() }
                    if (uidList.isEmpty()) { renderTabla(); return }

                    var cargados = 0
                    for (uid in uidList) {
                        db.child("usuarios").child(uid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnap: DataSnapshot) {
                                    val nombre    = userSnap.child("nombre").getValue(String::class.java) ?: uid
                                    val matricula = userSnap.child("matricula").getValue(String::class.java) ?: ""
                                    db.child("asistencias").child(grupoId).child(uid)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(asSnap: DataSnapshot) {
                                                var presentes = 0; var faltas = 0
                                                val asistenciasPorFecha = mutableMapOf<String, Boolean>()
                                                for (dia in asSnap.children) {
                                                    val fecha = dia.key ?: continue
                                                    val presente = dia.child("presente").getValue(Boolean::class.java) ?: false
                                                    asistenciasPorFecha[fecha] = presente
                                                    if (!todasFechas.contains(fecha)) todasFechas.add(fecha)
                                                    if (presente) presentes++ else faltas++
                                                }
                                                filas.add(RowAlumno(uid, nombre, matricula, presentes, faltas, asistenciasPorFecha))
                                                cargados++
                                                if (cargados == uidList.size) {
                                                    todasFechas.sort()
                                                    filas.sortBy { it.nombre }
                                                    renderTabla()
                                                }
                                            }
                                            override fun onCancelled(e: DatabaseError) { cargados++ }
                                        })
                                }
                                override fun onCancelled(e: DatabaseError) { cargados++ }
                            })
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun renderTabla() {
        tableLayout.removeAllViews()
        val colorHeader = Color.parseColor("#4F6BED")
        val colorAlerta = Color.parseColor("#FFDDDD")
        val colorNormal = Color.parseColor("#DDFFDD")
        val colorAlt    = Color.parseColor("#F5F5F5")

        fun makeHeader(text: String): TextView {
            return TextView(this).apply {
                this.text = text
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                textSize = 11f
                gravity = Gravity.CENTER
                setPadding(8, 10, 8, 10)
                minWidth = 80
            }
        }

        fun makeCell(text: String, bold: Boolean = false, color: Int = Color.BLACK): TextView {
            return TextView(this).apply {
                this.text = text
                setTextColor(color)
                if (bold) setTypeface(null, Typeface.BOLD)
                textSize = 11f
                gravity = Gravity.CENTER
                setPadding(8, 8, 8, 8)
                minWidth = 80
            }
        }

        val headerRow = TableRow(this)
        headerRow.setBackgroundColor(colorHeader)
        headerRow.addView(makeHeader("Matrícula"))
        headerRow.addView(makeHeader("Nombre"))
        for (fecha in todasFechas) {
            val fmt  = SimpleDateFormat("dd/MM", Locale.getDefault())
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fecha)
            headerRow.addView(makeHeader(if (date != null) fmt.format(date) else fecha))
        }
        headerRow.addView(makeHeader("Total"))
        headerRow.addView(makeHeader("%"))
        tableLayout.addView(headerRow)

        for ((i, fila) in filas.withIndex()) {
            val total = fila.presentes + fila.faltas
            val pct   = if (total > 0) fila.presentes * 100 / total else 0
            val row   = TableRow(this)
            row.setBackgroundColor(if (fila.faltas >= 2) colorAlerta else if (i % 2 == 0) colorNormal else colorAlt)
            row.addView(makeCell(fila.matricula, bold = true))
            row.addView(makeCell(fila.nombre))
            for (fecha in todasFechas) {
                val presente = fila.asistenciasPorFecha[fecha]
                val (symbol, color) = when (presente) {
                    true  -> Pair("✓", Color.parseColor("#1B5E20"))
                    false -> Pair("✗", Color.parseColor("#B71C1C"))
                    null  -> Pair("—", Color.GRAY)
                }
                row.addView(makeCell(symbol, color = color))
            }
            row.addView(makeCell("${fila.presentes}/$total"))
            row.addView(makeCell("$pct%", bold = fila.faltas >= 2,
                color = if (fila.faltas >= 2) Color.parseColor("#B71C1C")
                else Color.parseColor("#1B5E20")))
            tableLayout.addView(row)
        }

        if (filas.isEmpty()) {
            val emptyRow = TableRow(this)
            emptyRow.addView(TextView(this).apply {
                text = "No hay alumnos registrados"
                setTextColor(Color.GRAY)
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(16, 40, 16, 40)
            })
            tableLayout.addView(emptyRow)
        }
    }
}