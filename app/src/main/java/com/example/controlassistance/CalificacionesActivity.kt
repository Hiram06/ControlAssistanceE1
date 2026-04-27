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
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

data class FilaCalificacion(
    val uid: String,
    val nombre: String,
    val matricula: String,
    val calificaciones: MutableMap<String, String> = mutableMapOf(),
    var final: String = ""
)

data class ComponenteConfig(
    val key: String,
    val nombre: String,
    val porcentaje: Double,
    val cantidad: Int
)

class CalificacionesActivity : AppCompatActivity() {

    private val db          = FirebaseDatabase.getInstance().reference
    private val filas       = mutableListOf<FilaCalificacion>()
    private val componentes = mutableListOf<ComponenteConfig>()
    private val columnas    = mutableListOf<String>() // claves de cada columna
    private lateinit var grupoId: String
    private lateinit var grupoNombre: String
    private lateinit var tableLayout: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calificaciones)

        grupoId     = intent.getStringExtra("grupoId") ?: ""
        grupoNombre = intent.getStringExtra("grupoNombre") ?: ""

        tableLayout = findViewById(R.id.tableCalificaciones)
        findViewById<TextView>(R.id.tvTituloCalificaciones).text = grupoNombre
        findViewById<Button>(R.id.btnVolverCal).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnGuardarCal).setOnClickListener { guardarCalificaciones() }
        findViewById<Button>(R.id.btnPdfCal).setOnClickListener { generarPdf() }
        findViewById<Button>(R.id.btnConfigurarCal).setOnClickListener {
            val intent = android.content.Intent(this, ConfigurarCalificacionesActivity::class.java)
            intent.putExtra("grupoId", grupoId)
            intent.putExtra("grupoNombre", grupoNombre)
            startActivity(intent)
        }

        cargarConfigYDatos()
    }

    override fun onResume() {
        super.onResume()
        cargarConfigYDatos()
    }

    private fun cargarConfigYDatos() {
        db.child("config_calificaciones").child(grupoId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    componentes.clear()
                    columnas.clear()

                    if (!snapshot.exists()) {
                        // Sin configuración
                        tableLayout.removeAllViews()
                        val tv = TextView(this@CalificacionesActivity).apply {
                            text = "Configura el esquema de calificaciones primero"
                            textSize = 14f
                            setTextColor(Color.GRAY)
                            gravity = Gravity.CENTER
                            setPadding(16, 40, 16, 40)
                        }
                        val row = android.widget.TableRow(this@CalificacionesActivity)
                        row.addView(tv)
                        tableLayout.addView(row)
                        return
                    }

                    // Cargar componentes ordenados
                    val snapList = snapshot.children.toList()
                    for (snap in snapList) {
                        val nombre     = snap.child("nombre").getValue(String::class.java) ?: ""
                        val porcentaje = snap.child("porcentaje").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                        val cantidad   = snap.child("cantidad").getValue(Int::class.java) ?: 1
                        val comp = ComponenteConfig(snap.key ?: "", nombre, porcentaje, cantidad)
                        componentes.add(comp)
                        for (i in 1..cantidad) {
                            columnas.add("${snap.key}_$i")
                        }
                    }

                    cargarAlumnos()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun cargarAlumnos() {
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

                                    db.child("calificaciones").child(grupoId).child(uid)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(calSnap: DataSnapshot) {
                                                val cals = mutableMapOf<String, String>()
                                                for (col in columnas) {
                                                    cals[col] = calSnap.child(col).getValue(String::class.java) ?: ""
                                                }
                                                val final = calSnap.child("final").getValue(String::class.java) ?: ""
                                                filas.add(FilaCalificacion(uid, nombre, matricula, cals, final))
                                                cargados++
                                                if (cargados == uidList.size) {
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
        val colorHeader = Color.parseColor("#27AE60")

        fun makeHeader(text: String, minW: Int = 90): TextView {
            return TextView(this).apply {
                this.text = text
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                textSize = 10f
                gravity = Gravity.CENTER
                setPadding(6, 8, 6, 8)
                minWidth = minW
            }
        }

        // Encabezados
        val headerRow = android.widget.TableRow(this)
        headerRow.setBackgroundColor(colorHeader)
        headerRow.addView(makeHeader("Matrícula"))
        headerRow.addView(makeHeader("Nombre", 140))

        for (comp in componentes) {
            for (i in 1..comp.cantidad) {
                val label = if (comp.cantidad > 1) "${comp.nombre} $i\n(${comp.porcentaje.toInt()/comp.cantidad}%)"
                else "${comp.nombre}\n(${comp.porcentaje.toInt()}%)"
                headerRow.addView(makeHeader(label))
            }
        }
        headerRow.addView(makeHeader("Final"))
        tableLayout.addView(headerRow)

        // Filas de alumnos
        for ((i, fila) in filas.withIndex()) {
            val row = android.widget.TableRow(this)
            row.setBackgroundColor(if (i % 2 == 0) Color.WHITE else Color.parseColor("#F5F5F5"))

            fun makeCell(text: String): TextView {
                return TextView(this).apply {
                    this.text = text
                    setTextColor(Color.BLACK)
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setPadding(6, 8, 6, 8)
                    minWidth = 90
                }
            }

            fun makeInput(colKey: String): EditText {
                return EditText(this).apply {
                    setText(fila.calificaciones[colKey] ?: "")
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setPadding(4, 4, 4, 4)
                    minWidth = 70
                    maxWidth = 100
                    addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            fila.calificaciones[colKey] = s?.toString() ?: ""
                            calcularFinal(fila)
                        }
                        override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                        override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                    })
                }
            }

            row.addView(makeCell(fila.matricula))
            row.addView(makeCell(fila.nombre).also { it.minWidth = 140 })

            for (col in columnas) {
                row.addView(makeInput(col))
            }

            val tvFinal = TextView(this).apply {
                text = fila.final
                setTextColor(Color.parseColor("#27AE60"))
                setTypeface(null, Typeface.BOLD)
                textSize = 10f
                gravity = Gravity.CENTER
                setPadding(6, 8, 6, 8)
                minWidth = 90
                tag = "final_${fila.uid}"
            }
            row.addView(tvFinal)
            tableLayout.addView(row)
        }
    }

    private fun calcularFinal(fila: FilaCalificacion) {
        var total = 0.0
        var completo = true

        for (comp in componentes) {
            val pctPorItem = comp.porcentaje / comp.cantidad
            for (i in 1..comp.cantidad) {
                val key = "${comp.key}_$i"
                val val_ = fila.calificaciones[key]?.toDoubleOrNull()
                if (val_ == null) { completo = false; break }
                total += val_ * (pctPorItem / 100.0)
            }
        }

        if (completo) {
            fila.final = "%.1f".format(total)
            val tvFinal = tableLayout.findViewWithTag<TextView>("final_${fila.uid}")
            tvFinal?.text = fila.final
        }
    }

    private fun guardarCalificaciones() {
        for (fila in filas) {
            val data = mutableMapOf<String, Any>()
            for ((key, value) in fila.calificaciones) {
                data[key] = value
            }
            data["final"] = fila.final
            db.child("calificaciones").child(grupoId).child(fila.uid).setValue(data)
        }
        Toast.makeText(this, "Calificaciones guardadas", Toast.LENGTH_SHORT).show()
    }

    private fun generarPdf() {
        val fileName = "Calificaciones_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"
        val doc      = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
        val page     = doc.startPage(pageInfo)
        val canvas   = page.canvas
        val bold     = Paint().apply { isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
        val normal   = Paint().apply { isAntiAlias = true; typeface = Typeface.DEFAULT }

        val hPaint = Paint().apply { color = Color.parseColor("#27AE60") }
        canvas.drawRect(0f, 0f, 842f, 60f, hPaint)
        bold.color = Color.WHITE; bold.textSize = 18f
        canvas.drawText("CALIFICACIONES — $grupoNombre", 40f, 42f, bold)

        val thBg = Paint().apply { color = Color.parseColor("#E8F5E9") }
        canvas.drawRect(30f, 70f, 812f, 90f, thBg)
        bold.color = Color.BLACK; bold.textSize = 10f
        var x = 35f
        canvas.drawText("Matrícula", x, 84f, bold); x += 80f
        canvas.drawText("Nombre", x, 84f, bold); x += 150f
        for (comp in componentes) {
            for (i in 1..comp.cantidad) {
                val label = if (comp.cantidad > 1) "${comp.nombre}$i" else comp.nombre
                canvas.drawText(label, x, 84f, bold); x += 55f
            }
        }
        canvas.drawText("Final", x, 84f, bold)

        normal.textSize = 9f
        var y = 108f
        for ((i, fila) in filas.withIndex()) {
            if (i % 2 == 0) {
                val rowBg = Paint().apply { color = Color.parseColor("#FAFAFA") }
                canvas.drawRect(30f, y - 12f, 812f, y + 5f, rowBg)
            }
            normal.color = Color.DKGRAY
            x = 35f
            canvas.drawText(fila.matricula, x, y, normal); x += 80f
            canvas.drawText(fila.nombre.take(22), x, y, normal); x += 150f
            for (col in columnas) {
                canvas.drawText(fila.calificaciones[col]?.ifEmpty { "—" } ?: "—", x, y, normal)
                x += 55f
            }
            bold.color = Color.parseColor("#27AE60"); bold.textSize = 9f
            canvas.drawText(fila.final.ifEmpty { "—" }, x, y, bold)
            y += 20f
            if (y > 575f) break
        }

        doc.finishPage(page)

        try {
            val outputStream = getOutputStream(fileName)
            if (outputStream == null) {
                Toast.makeText(this, "No se pudo crear el PDF", Toast.LENGTH_LONG).show()
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