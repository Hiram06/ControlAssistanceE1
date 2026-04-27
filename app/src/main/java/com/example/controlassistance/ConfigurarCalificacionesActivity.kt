package com.example.controlassistance

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class ConfigurarCalificacionesActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance().reference
    private lateinit var grupoId: String
    private lateinit var grupoNombre: String
    private lateinit var llComponentes: LinearLayout
    private var componentes = mutableListOf<ComponenteCalificacion>()

    data class ComponenteCalificacion(
        var nombre: String = "",
        var porcentaje: String = "",
        var cantidad: Int = 1
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configurar_calificaciones)

        grupoId     = intent.getStringExtra("grupoId") ?: ""
        grupoNombre = intent.getStringExtra("grupoNombre") ?: ""

        llComponentes = findViewById(R.id.llComponentes)

        findViewById<TextView>(R.id.tvTituloConfig).text = "Configurar: $grupoNombre"
        findViewById<Button>(R.id.btnVolverConfig).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAgregarComponente).setOnClickListener { agregarComponente() }
        findViewById<Button>(R.id.btnGuardarConfig).setOnClickListener { guardarConfiguracion() }

        cargarConfiguracion()
    }

    private fun cargarConfiguracion() {
        db.child("config_calificaciones").child(grupoId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    componentes.clear()
                    for (snap in snapshot.children) {
                        val nombre     = snap.child("nombre").getValue(String::class.java) ?: ""
                        val porcentaje = snap.child("porcentaje").getValue(String::class.java) ?: ""
                        val cantidad   = snap.child("cantidad").getValue(Int::class.java) ?: 1
                        componentes.add(ComponenteCalificacion(nombre, porcentaje, cantidad))
                    }
                    if (componentes.isEmpty()) {
                        // Configuración por defecto
                        componentes.add(ComponenteCalificacion("Examen", "40", 2))
                        componentes.add(ComponenteCalificacion("Actividades", "40", 3))
                        componentes.add(ComponenteCalificacion("PIA", "20", 1))
                    }
                    renderComponentes()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun renderComponentes() {
        llComponentes.removeAllViews()
        for ((index, comp) in componentes.withIndex()) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 8) }
            }

            val etNombre = EditText(this).apply {
                setText(comp.nombre)
                hint = "Nombre"
                textSize = 13f
                setPadding(8, 8, 8, 8)
                setBackgroundColor(android.graphics.Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                addTextChangedListener(object : android.text.TextWatcher {
                    override fun afterTextChanged(s: android.text.Editable?) { comp.nombre = s?.toString() ?: "" }
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                })
            }

            val etPorcentaje = EditText(this).apply {
                setText(comp.porcentaje)
                hint = "%"
                textSize = 13f
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setPadding(8, 8, 8, 8)
                setBackgroundColor(android.graphics.Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .also { it.setMargins(8, 0, 8, 0) }
                addTextChangedListener(object : android.text.TextWatcher {
                    override fun afterTextChanged(s: android.text.Editable?) { comp.porcentaje = s?.toString() ?: "" }
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                })
            }

            val etCantidad = EditText(this).apply {
                setText(comp.cantidad.toString())
                hint = "Cant"
                textSize = 13f
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setPadding(8, 8, 8, 8)
                setBackgroundColor(android.graphics.Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .also { it.setMargins(0, 0, 8, 0) }
                addTextChangedListener(object : android.text.TextWatcher {
                    override fun afterTextChanged(s: android.text.Editable?) {
                        comp.cantidad = s?.toString()?.toIntOrNull() ?: 1
                    }
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                })
            }

            val btnEliminar = Button(this).apply {
                text = "✕"
                textSize = 12f
                setBackgroundColor(android.graphics.Color.parseColor("#C0392B"))
                setTextColor(android.graphics.Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    componentes.removeAt(index)
                    renderComponentes()
                }
            }

            row.addView(etNombre)
            row.addView(etPorcentaje)
            row.addView(etCantidad)
            row.addView(btnEliminar)
            llComponentes.addView(row)
        }

        // Mostrar total de porcentaje
        val total = componentes.sumOf { it.porcentaje.toIntOrNull() ?: 0 }
        val tvTotal = findViewById<TextView>(R.id.tvTotalPorcentaje)
        tvTotal.text = "Total: $total%"
        tvTotal.setTextColor(
            if (total == 100) android.graphics.Color.parseColor("#1B5E20")
            else android.graphics.Color.parseColor("#B71C1C")
        )
    }

    private fun agregarComponente() {
        componentes.add(ComponenteCalificacion("", "", 1))
        renderComponentes()
    }

    private fun guardarConfiguracion() {
        val total = componentes.sumOf { it.porcentaje.toIntOrNull() ?: 0 }
        if (total != 100) {
            Toast.makeText(this, "Los porcentajes deben sumar 100%. Actual: $total%", Toast.LENGTH_LONG).show()
            return
        }
        if (componentes.any { it.nombre.isEmpty() }) {
            Toast.makeText(this, "Todos los componentes deben tener nombre", Toast.LENGTH_SHORT).show()
            return
        }

        val config = mutableMapOf<String, Any>()
        for ((i, comp) in componentes.withIndex()) {
            config["comp_$i"] = mapOf(
                "nombre"     to comp.nombre,
                "porcentaje" to comp.porcentaje,
                "cantidad"   to comp.cantidad
            )
        }

        db.child("config_calificaciones").child(grupoId).setValue(config)
            .addOnSuccessListener {
                Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}