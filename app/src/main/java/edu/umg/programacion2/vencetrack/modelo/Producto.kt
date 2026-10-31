
package edu.umg.programacion2.vencetrack.modelo

import java.time.LocalDate

data class Producto(
    val alterno: String,
    val descripcion: String,
    val cantidad: Int,
    val fechaVencimiento: LocalDate,
    val seguimientos: List<Seguimiento> = emptyList()
)
