package com.example.controlassistance

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

data class RowAlumno(
    val uid: String,
    val nombre: String,
    val matricula: String,
    var presentes: Int,
    var faltas: Int,
    val asistenciasPorFecha: MutableMap<String, Boolean> = mutableMapOf()
)

class ReporteGrupoActivity : AppCompatActivity() {

    private val db      = FirebaseDatabase.getInstance().reference
    private val filas   = mutableListOf<RowAlumno>()
    private lateinit var grupoId: String
    private lateinit var grupoNombre: String
    private lateinit var tableLayout: TableLayout
    private val todasFechas = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte_grupo)

        grupoId     = intent.getStringExtra("grupoId") ?: ""
        grupoNombre = intent.getStringExtra("grupoNombre") ?: ""

        findViewById<TextView>(R.id.tvTituloReporteGrupo).text = grupoNombre
        tableLayout = findViewById(R.id.tableAsistencia)

        findViewById<Button>(R.id.btnVolverReporte).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnEliminarFaltas).setOnClickListener { confirmarEliminar() }
        findViewById<Button>(R.id.btnPdfGrupo).setOnClickListener { generarPdfGrupo() }

        cargarReporte()
    }

    private fun cargarReporte() {
        db.child("grupos").child(grupoId).child("alumnos")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    filas.clear()
                    todasFechas.clear()
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

        // ── Resumen del grupo ──────────────────────────────────────────────
        val totalAlumnos    = filas.size
        val conMasFaltas    = filas.count { it.faltas >= 2 }
        val diasRegistrados = todasFechas.size
        val promedioAsist   = if (filas.isNotEmpty()) {
            filas.map { f ->
                val tot = f.presentes + f.faltas
                if (tot > 0) f.presentes * 100 / tot else 0
            }.average().toInt()
        } else 0

        findViewById<TextView>(R.id.tvResumenAlumnos).text  = "Alumnos\n$totalAlumnos"
        findViewById<TextView>(R.id.tvResumenFaltas).text   = "Con 2+ faltas\n$conMasFaltas"
        findViewById<TextView>(R.id.tvResumenPromedio).text = "Promedio grupo\n$promedioAsist%"
        findViewById<TextView>(R.id.tvResumenDias).text     = "Dias registrados\n$diasRegistrados"

        val colorHeader  = Color.parseColor("#4F6BED")
        val colorAlerta  = Color.parseColor("#FFDDDD")
        val colorNormal  = Color.parseColor("#DDFFDD")
        val colorAlterno = Color.parseColor("#F5F5F5")

        // Fila de encabezados
        val headerRow = TableRow(this)
        headerRow.setBackgroundColor(colorHeader)

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

        headerRow.addView(makeHeader("Matrícula"))
        headerRow.addView(makeHeader("Nombre"))
        for (fecha in todasFechas) {
            val fmt = SimpleDateFormat("dd/MM", Locale.getDefault())
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fecha)
            headerRow.addView(makeHeader(if (date != null) fmt.format(date) else fecha))
        }
        headerRow.addView(makeHeader("Total"))
        headerRow.addView(makeHeader("%"))
        tableLayout.addView(headerRow)

        // Filas de alumnos
        for ((i, fila) in filas.withIndex()) {
            val total = fila.presentes + fila.faltas
            val pct   = if (total > 0) fila.presentes * 100 / total else 0
            val row   = TableRow(this)
            row.setBackgroundColor(if (fila.faltas >= 2) colorAlerta else if (i % 2 == 0) colorNormal else colorAlterno)

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
                color = if (fila.faltas >= 2) Color.parseColor("#B71C1C") else Color.parseColor("#1B5E20")))

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

    private fun confirmarEliminar() {
        val conFaltas = filas.filter { it.faltas >= 2 }
        if (conFaltas.isEmpty()) {
            Toast.makeText(this, "No hay alumnos con 2+ faltas", Toast.LENGTH_SHORT).show(); return
        }
        val nombres = conFaltas.joinToString("\n") { "- ${it.nombre}" }
        AlertDialog.Builder(this)
            .setTitle("Eliminar alumnos con 2+ faltas")
            .setMessage("Se eliminarán del grupo:\n\n$nombres")
            .setPositiveButton("Eliminar") { _, _ ->
                for (row in conFaltas)
                    db.child("grupos").child(grupoId).child("alumnos").child(row.uid).removeValue()
                filas.removeAll { it.faltas >= 2 }
                renderTabla()
                Toast.makeText(this, "${conFaltas.size} alumno(s) eliminado(s)", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun generarPdfGrupo() {
        val fileName = "Reporte_Grupo_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"
        val doc      = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create() // Horizontal
        val page     = doc.startPage(pageInfo)
        val canvas   = page.canvas
        val bold     = Paint().apply { isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
        val normal   = Paint().apply { isAntiAlias = true; typeface = Typeface.DEFAULT }
        val line     = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        val hPaint = Paint().apply { color = Color.parseColor("#4F6BED") }
        canvas.drawRect(0f, 0f, 842f, 60f, hPaint)
        bold.color = Color.WHITE; bold.textSize = 18f
        canvas.drawText("LISTA DE ASISTENCIA — $grupoNombre", 40f, 40f, bold)

        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        normal.color = Color.WHITE; normal.textSize = 11f
        canvas.drawText("Generado: $fecha", 650f, 40f, normal)

        // Encabezados tabla
        val thBg = Paint().apply { color = Color.parseColor("#E8ECF8") }
        canvas.drawRect(30f, 70f, 812f, 90f, thBg)
        bold.color = Color.BLACK; bold.textSize = 10f
        var x = 35f
        canvas.drawText("Matrícula", x, 84f, bold); x += 80f
        canvas.drawText("Nombre", x, 84f, bold); x += 160f
        for (f in todasFechas) {
            val fmt = SimpleDateFormat("dd/MM", Locale.getDefault())
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(f)
            canvas.drawText(if (d != null) fmt.format(d) else f, x, 84f, bold)
            x += 40f
        }
        canvas.drawText("Total", x, 84f, bold); x += 50f
        canvas.drawText("%", x, 84f, bold)

        // Filas
        normal.textSize = 10f
        var y = 105f
        for ((i, fila) in filas.withIndex()) {
            val tot = fila.presentes + fila.faltas
            val pct = if (tot > 0) fila.presentes * 100 / tot else 0
            if (i % 2 == 0) {
                val rowBg = Paint().apply { color = Color.parseColor("#FAFAFA") }
                canvas.drawRect(30f, y - 12f, 812f, y + 5f, rowBg)
            }
            normal.color = if (fila.faltas >= 2) Color.parseColor("#C0392B") else Color.BLACK
            x = 35f
            canvas.drawText(fila.matricula, x, y, normal); x += 80f
            canvas.drawText(fila.nombre.take(20), x, y, normal); x += 160f
            for (f in todasFechas) {
                val presente = fila.asistenciasPorFecha[f]
                canvas.drawText(when (presente) { true -> "✓"; false -> "✗"; else -> "—" }, x, y, normal)
                x += 40f
            }
            canvas.drawText("${fila.presentes}/$tot", x, y, normal); x += 50f
            canvas.drawText("$pct%", x, y, normal)
            y += 20f
            if (y > 570f) break
        }

        doc.finishPage(page)

        try {
            val outputStream = getOutputStream(fileName)
            if (outputStream == null) {
                Toast.makeText(this, "No se pudo crear el archivo", Toast.LENGTH_LONG).show()
                doc.close(); return
            }
            doc.writeTo(outputStream)
            doc.close(); outputStream.close()
            Toast.makeText(this, "PDF guardado: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            doc.close()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getOutputStream(fileName: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
            uri?.let { contentResolver.openOutputStream(it) }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            FileOutputStream(File(dir, fileName))
        }
    }
}