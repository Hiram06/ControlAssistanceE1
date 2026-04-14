package com.example.controlassistance

import android.content.ContentValues
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
    var faltas: Int
)

class ReporteGrupoActivity : AppCompatActivity() {

    private val db      = FirebaseDatabase.getInstance().reference
    private val filas   = mutableListOf<RowAlumno>()
    private lateinit var grupoId: String
    private lateinit var grupoNombre: String
    private lateinit var llAlumnos: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte_grupo)

        grupoId     = intent.getStringExtra("grupoId") ?: ""
        grupoNombre = intent.getStringExtra("grupoNombre") ?: ""

        findViewById<TextView>(R.id.tvTituloReporteGrupo).text = grupoNombre
        llAlumnos = findViewById(R.id.llAlumnos)

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
                                                for (dia in asSnap.children) {
                                                    if (dia.child("presente").getValue(Boolean::class.java) == true)
                                                        presentes++ else faltas++
                                                }
                                                filas.add(RowAlumno(uid, nombre, matricula, presentes, faltas))
                                                cargados++
                                                if (cargados == uidList.size) { filas.sortBy { it.nombre }; renderTabla() }
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
        llAlumnos.removeAllViews()
        if (filas.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No hay alumnos registrados en este grupo."
                textSize = 14f
                setTextColor(Color.GRAY)
                gravity = android.view.Gravity.CENTER
                setPadding(16, 40, 16, 40)
            }
            llAlumnos.addView(tv); return
        }
        for (fila in filas) {
            val total = fila.presentes + fila.faltas
            val pct   = if (total > 0) fila.presentes * 100 / total else 0
            val card  = layoutInflater.inflate(R.layout.item_alumno_reporte, llAlumnos, false)
            card.findViewById<TextView>(R.id.tvNombreAlumno).text = "${fila.nombre} (${fila.matricula})"
            card.findViewById<TextView>(R.id.tvAsistenciaAlumno).text =
                "Asistencias: ${fila.presentes}/$total ($pct%)  |  Faltas: ${fila.faltas}"
            if (fila.faltas >= 2) {
                card.setBackgroundColor(Color.parseColor("#FFDDDD"))
                card.findViewById<TextView>(R.id.tvAlertaAlumno).visibility = android.view.View.VISIBLE
            } else {
                card.setBackgroundColor(Color.parseColor("#DDFFDD"))
                card.findViewById<TextView>(R.id.tvAlertaAlumno).visibility = android.view.View.GONE
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, 10) }
            card.layoutParams = params
            llAlumnos.addView(card)
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
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page     = doc.startPage(pageInfo)
        val canvas   = page.canvas
        val bold     = Paint().apply { isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
        val normal   = Paint().apply { isAntiAlias = true; typeface = Typeface.DEFAULT }
        val line     = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        val hPaint = Paint().apply { color = Color.parseColor("#4F6BED") }
        canvas.drawRect(0f, 0f, 595f, 80f, hPaint)
        bold.color = Color.WHITE; bold.textSize = 18f
        canvas.drawText("LISTA DE ASISTENCIA", 160f, 52f, bold)

        normal.color = Color.BLACK; normal.textSize = 12f
        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Grupo: $grupoNombre", 60f, 100f, normal)
        canvas.drawText("Fecha: $fecha", 60f, 118f, normal)
        canvas.drawLine(60f, 128f, 535f, 128f, line)

        val conProblema = filas.count { it.faltas >= 2 }
        normal.color = Color.DKGRAY; normal.textSize = 11f
        canvas.drawText("Total de alumnos: ${filas.size}   |   Con 2+ faltas: $conProblema", 60f, 148f, normal)
        canvas.drawLine(60f, 158f, 535f, 158f, line)

        val thBg = Paint().apply { color = Color.parseColor("#E8ECF8") }
        canvas.drawRect(55f, 165f, 540f, 183f, thBg)
        bold.color = Color.BLACK; bold.textSize = 11f
        canvas.drawText("#", 62f, 178f, bold)
        canvas.drawText("Nombre", 80f, 178f, bold)
        canvas.drawText("Matricula", 280f, 178f, bold)
        canvas.drawText("Asist.", 380f, 178f, bold)
        canvas.drawText("Faltas", 430f, 178f, bold)
        canvas.drawText("%", 495f, 178f, bold)

        normal.textSize = 10f
        var y = 198f
        for ((i, fila) in filas.withIndex()) {
            val tot = fila.presentes + fila.faltas
            val pct = if (tot > 0) fila.presentes * 100 / tot else 0
            if (i % 2 == 0) {
                val rowBg = Paint().apply { color = Color.parseColor("#FAFAFA") }
                canvas.drawRect(55f, y - 13f, 540f, y + 5f, rowBg)
            }
            normal.color = if (fila.faltas >= 2) Color.parseColor("#C0392B") else Color.DKGRAY
            canvas.drawText("${i + 1}", 62f, y, normal)
            canvas.drawText(fila.nombre.take(26), 80f, y, normal)
            canvas.drawText(fila.matricula, 280f, y, normal)
            canvas.drawText("${fila.presentes}/$tot", 380f, y, normal)
            canvas.drawText("${fila.faltas}", 435f, y, normal)
            canvas.drawText("$pct%", 495f, y, normal)
            y += 20f
            if (y > 815f) break
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