package com.example.controlassistance

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MaestroQRActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maestro_qr)

        val btnCerrar = findViewById<Button>(R.id.btnCerrarQR)

        btnCerrar.setOnClickListener {
            finish()
        }
    }
}