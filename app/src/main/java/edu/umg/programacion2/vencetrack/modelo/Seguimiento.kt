
package edu.umg.programacion2.vencetrack.modelo

import java.time.LocalDate

data class Seguimiento(
    val tipo: TipoSeguimiento,
    val fechaProgramada: LocalDate,
    val completado: Boolean = false,
    val omitido: Boolean = false
)
