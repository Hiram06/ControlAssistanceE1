package com.example.controlassistance

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class SolicitudesActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance().reference
    private lateinit var grupoId: String
    private lateinit var llSolicitudes: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitudes)

        grupoId = intent.getStringExtra("grupoId") ?: ""
        val grupoNombre = intent.getStringExtra("grupoNombre") ?: ""

        llSolicitudes = findViewById(R.id.llSolicitudes)
        findViewById<TextView>(R.id.tvTituloSolicitudes).text = "Solicitudes: $grupoNombre"
        findViewById<Button>(R.id.btnVolverSolicitudes).setOnClickListener { finish() }

        cargarSolicitudes()
    }

    private fun cargarSolicitudes() {
        db.child("solicitudes").child(grupoId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    llSolicitudes.removeAllViews()
                    var haySolicitudes = false

                    for (snap in snapshot.children) {
                        val alumnoId = snap.key ?: continue
                        val nombre   = snap.child("nombre").getValue(String::class.java) ?: ""
                        val estado   = snap.child("estado").getValue(String::class.java) ?: ""

                        if (estado != "pendiente") continue
                        haySolicitudes = true

                        val row = LinearLayout(this@SolicitudesActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(16, 12, 16, 12)
                            setBackgroundColor(android.graphics.Color.WHITE)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).also { it.setMargins(0, 0, 0, 8) }
                        }

                        val tvNombre = TextView(this@SolicitudesActivity).apply {
                            text = nombre
                            textSize = 15f
                            setTextColor(android.graphics.Color.BLACK)
                            layoutParams = LinearLayout.LayoutParams(0,
                                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }

                        val btnAprobar = Button(this@SolicitudesActivity).apply {
                            text = "Aprobar"
                            setBackgroundColor(android.graphics.Color.parseColor("#27AE60"))
                            setTextColor(android.graphics.Color.WHITE)
                            textSize = 12f
                            setPadding(16, 8, 16, 8)
                            setOnClickListener {
                                // Inscribir al alumno en el grupo
                                db.child("grupos").child(grupoId)
                                    .child("alumnos").child(alumnoId).setValue(true)
                                // Actualizar estado de solicitud
                                db.child("solicitudes").child(grupoId)
                                    .child(alumnoId).child("estado").setValue("aprobada")
                                Toast.makeText(this@SolicitudesActivity,
                                    "$nombre fue aprobado", Toast.LENGTH_SHORT).show()
                            }
                        }

                        val btnRechazar = Button(this@SolicitudesActivity).apply {
                            text = "Rechazar"
                            setBackgroundColor(android.graphics.Color.parseColor("#C0392B"))
                            setTextColor(android.graphics.Color.WHITE)
                            textSize = 12f
                            setPadding(16, 8, 16, 8)
                            setOnClickListener {
                                db.child("solicitudes").child(grupoId)
                                    .child(alumnoId).child("estado").setValue("rechazada")
                                Toast.makeText(this@SolicitudesActivity,
                                    "Solicitud de $nombre rechazada", Toast.LENGTH_SHORT).show()
                            }
                        }

                        row.addView(tvNombre)
                        row.addView(btnAprobar)
                        row.addView(btnRechazar)
                        llSolicitudes.addView(row)
                    }

                    if (!haySolicitudes) {
                        val tv = TextView(this@SolicitudesActivity).apply {
                            text = "No hay solicitudes pendientes"
                            textSize = 14f
                            setTextColor(android.graphics.Color.GRAY)
                            gravity = android.view.Gravity.CENTER
                            setPadding(16, 40, 16, 40)
                        }
                        llSolicitudes.addView(tv)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}