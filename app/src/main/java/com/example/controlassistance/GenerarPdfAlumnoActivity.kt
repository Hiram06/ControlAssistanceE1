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
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class GenerarPdfAlumnoActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generar_pdf_alumno)

        val uid     = intent.getStringExtra("uid") ?: auth.currentUser?.uid ?: ""
        val grupoId = intent.getStringExtra("grupoId") ?: ""

        findViewById<Button>(R.id.btnVolverPdf).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnGenerarPdf).setOnClickListener {
            if (uid.isEmpty() || grupoId.isEmpty()) {
                Toast.makeText(this, "No se encontraron datos del alumno.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            generarPdf(uid, grupoId)
        }
    }

    private fun generarPdf(uid: String, grupoId: String) {
        db.child("usuarios").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnap: DataSnapshot) {
                    val nombre    = userSnap.child("nombre").getValue(String::class.java) ?: "Alumno"
                    val matricula = userSnap.child("matricula").getValue(String::class.java) ?: ""

                    db.child("asistencias").child(grupoId).child(uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(asSnap: DataSnapshot) {
                                val registros = mutableListOf<Pair<String, Boolean>>()
                                for (dia in asSnap.children) {
                                    val fecha    = dia.key ?: continue
                                    val presente = dia.child("presente").getValue(Boolean::class.java) ?: false
                                    registros.add(Pair(fecha, presente))
                                }
                                registros.sortBy { it.first }
                                crearYGuardarPdf(nombre, matricula, grupoId, registros)
                            }
                            override fun onCancelled(e: DatabaseError) {}
                        })
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun crearYGuardarPdf(
        nombre: String,
        matricula: String,
        grupoId: String,
        registros: List<Pair<String, Boolean>>
    ) {
        val presentes = registros.count { it.second }
        val faltas    = registros.count { !it.second }
        val total     = registros.size
        val pct       = if (total > 0) presentes * 100.0 / total else 0.0
        val fechaHoy  = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val fileName  = "Reporte_${matricula}_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"

        val doc      = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page     = doc.startPage(pageInfo)
        val canvas   = page.canvas
        val bold     = Paint().apply { isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
        val normal   = Paint().apply { isAntiAlias = true; typeface = Typeface.DEFAULT }

        val headerPaint = Paint().apply { color = Color.parseColor("#4F6BED") }
        canvas.drawRect(0f, 0f, 595f, 80f, headerPaint)
        bold.color = Color.WHITE; bold.textSize = 20f
        canvas.drawText("REPORTE DE ASISTENCIA", 130f, 52f, bold)

        normal.color = Color.DKGRAY; normal.textSize = 11f
        canvas.drawText("Generado: $fechaHoy", 60f, 100f, normal)

        bold.color = Color.BLACK; bold.textSize = 13f
        canvas.drawText("Alumno:    $nombre", 60f, 128f, bold)
        normal.color = Color.BLACK; normal.textSize = 13f
        canvas.drawText("Matricula: $matricula", 60f, 148f, normal)
        canvas.drawText("Grupo ID:  $grupoId", 60f, 168f, normal)

        val line = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        canvas.drawLine(60f, 180f, 535f, 180f, line)

        val boxPaint = Paint().apply { color = Color.parseColor("#F0F4FF") }
        canvas.drawRect(55f, 190f, 540f, 290f, boxPaint)
        bold.textSize = 14f; bold.color = Color.parseColor("#4F6BED")
        canvas.drawText("RESUMEN", 70f, 212f, bold)
        normal.textSize = 12f; normal.color = Color.BLACK
        canvas.drawText("Total de clases: $total", 70f, 235f, normal)
        canvas.drawText("Asistencias:     $presentes", 70f, 253f, normal)
        canvas.drawText("Faltas:          $faltas", 70f, 271f, normal)

        bold.textSize = 18f
        bold.color = if (pct >= 80) Color.parseColor("#1B5E20") else Color.parseColor("#B71C1C")
        canvas.drawText("${"%.1f".format(pct)}% de asistencia", 320f, 260f, bold)

        if (faltas >= 2) {
            val warnPaint = Paint().apply { color = Color.parseColor("#FFF3CD") }
            canvas.drawRect(55f, 296f, 540f, 318f, warnPaint)
            normal.color = Color.parseColor("#856404"); normal.textSize = 11f
            canvas.drawText("ADVERTENCIA: Tiene $faltas faltas acumuladas.", 65f, 311f, normal)
        }

        canvas.drawLine(60f, 326f, 535f, 326f, line)
        bold.textSize = 13f; bold.color = Color.BLACK
        canvas.drawText("DETALLE POR FECHA", 60f, 350f, bold)

        val headerBg = Paint().apply { color = Color.parseColor("#E8ECF8") }
        canvas.drawRect(55f, 358f, 540f, 378f, headerBg)
        bold.textSize = 11f
        canvas.drawText("Fecha", 65f, 373f, bold)
        canvas.drawText("Estado", 300f, 373f, bold)

        normal.textSize = 11f
        var y = 395f
        for ((i, registro) in registros.withIndex()) {
            val (fecha, presente) = registro
            if (i % 2 == 0) {
                val rowBg = Paint().apply { color = Color.parseColor("#FAFAFA") }
                canvas.drawRect(55f, y - 14f, 540f, y + 4f, rowBg)
            }
            normal.color = Color.DKGRAY
            canvas.drawText(fecha, 65f, y, normal)
            if (presente) {
                normal.color = Color.parseColor("#1B5E20")
                canvas.drawText("PRESENTE", 300f, y, normal)
            } else {
                normal.color = Color.parseColor("#B71C1C")
                canvas.drawText("FALTA", 300f, y, normal)
            }
            y += 20f
            if (y > 820f) break
        }

        doc.finishPage(page)

        try {
            val outputStream = getOutputStream(fileName)
            if (outputStream == null) {
                Toast.makeText(this, "No se pudo crear el archivo PDF", Toast.LENGTH_LONG).show()
                doc.close(); return
            }
            doc.writeTo(outputStream)
            doc.close(); outputStream.close()
            Toast.makeText(this, "PDF guardado: $fileName", Toast.LENGTH_LONG).show()
            finish()
        } catch (e: Exception) {
            doc.close()
            Toast.makeText(this, "Error al guardar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getOutputStream(fileName: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { contentResolver.openOutputStream(it) }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            FileOutputStream(File(dir, fileName))
        }
    }
}