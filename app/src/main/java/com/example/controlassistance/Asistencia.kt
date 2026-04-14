package com.example.controlassistance

data class Asistencia(
    val alumnoId: String = "",
    val grupoId: String = "",
    val fecha: String = "",
    val presente: Boolean = false
)