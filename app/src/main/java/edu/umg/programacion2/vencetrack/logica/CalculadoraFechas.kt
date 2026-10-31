
package edu.umg.programacion2.vencetrack.logica

import edu.umg.programacion2.vencetrack.modelo.EstadoVencimiento
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object CalculadoraFechas {

    fun diasRestantes(
        fecha: LocalDate,
        hoy: LocalDate = LocalDate.now()
    ): Long {
        return ChronoUnit.DAYS.between(hoy, fecha)
    }

    fun estadoVencimiento(
        fecha: LocalDate,
        hoy: LocalDate = LocalDate.now(),
        diasAviso: Long = 15
    ): EstadoVencimiento {

        val dias = diasRestantes(fecha, hoy)

        return when {
            dias < 0 -> EstadoVencimiento.VENCIDO
            dias == 0L -> EstadoVencimiento.VENCE_HOY
            dias <= diasAviso -> EstadoVencimiento.PROXIMO_A_VENCER
            else -> EstadoVencimiento.VIGENTE
        }
    }
}