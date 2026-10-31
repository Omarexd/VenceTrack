
package edu.umg.programacion2.vencetrack.logica

import edu.umg.programacion2.vencetrack.datos.ProductoEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class TipoEstadoVisual {
    VENCIDO,
    VENCE_HOY,
    REBAJA_ATRASADA,
    REBAJA_HOY,
    REBAJA_PROXIMA,
    COMPLETADO,
    PROXIMO_VENCER,
    VIGENTE
}

data class EstadoVisual(
    val texto: String,
    val tipo: TipoEstadoVisual
)

object EstadoVisualProducto {

    fun obtenerEstado(
        producto: ProductoEntity,
        hoy: LocalDate = LocalDate.now()
    ): EstadoVisual {

        val vencimiento = LocalDate.parse(
            producto.fechaVencimiento
        )

        if (vencimiento.isBefore(hoy)) {
            return EstadoVisual(
                "Vencido",
                TipoEstadoVisual.VENCIDO
            )
        }

        data class Rebaja(
            val porcentaje: Int,
            val fecha: LocalDate?,
            val completado: Boolean,
            val omitido: Boolean
        )

        val rebajas = listOf(
            Rebaja(
                25,
                producto.fecha25?.let { LocalDate.parse(it) },
                producto.completado25,
                producto.omitido25
            ),
            Rebaja(
                50,
                producto.fecha50?.let { LocalDate.parse(it) },
                producto.completado50,
                producto.omitido50
            ),
            Rebaja(
                75,
                producto.fecha75?.let { LocalDate.parse(it) },
                producto.completado75,
                producto.omitido75
            )
        )
        if (vencimiento.isEqual(hoy)) {

            val seguimientoFinalizado = rebajas.all {
                it.completado || it.omitido
            } && rebajas.any {
                it.completado
            }

            return if (seguimientoFinalizado) {
                EstadoVisual(
                    "Completado",
                    TipoEstadoVisual.COMPLETADO
                )
            } else {
                EstadoVisual(
                    "Vence hoy",
                    TipoEstadoVisual.VENCE_HOY
                )
            }
        }

        val pendientes = rebajas.filter {
            it.fecha != null && !it.completado && !it.omitido
        }

        // Primero comprobar rebajas atrasadas
        val atrasada = pendientes
            .filter { it.fecha!!.isBefore(hoy) }
            .minByOrNull { it.fecha!! }

        if (atrasada != null) {
            return EstadoVisual(
                "${atrasada.porcentaje}% atrasado",
                TipoEstadoVisual.REBAJA_ATRASADA
            )
        }

        // Rebajas programadas para hoy
        val paraHoy = pendientes.firstOrNull {
            it.fecha == hoy
        }

        if (paraHoy != null) {
            return EstadoVisual(
                "${paraHoy.porcentaje}% para hoy",
                TipoEstadoVisual.REBAJA_HOY
            )
        }

        // Rebajas dentro de los próximos 5 días
        val proxima = pendientes
            .filter {
                val fecha = it.fecha!!
                fecha.isAfter(hoy) &&
                        !fecha.isAfter(hoy.plusDays(5))
            }
            .minByOrNull { it.fecha!! }

        if (proxima != null) {
            return EstadoVisual(
                "${proxima.porcentaje}% próximo",
                TipoEstadoVisual.REBAJA_PROXIMA
            )
        }

        // Comprobar si todas las rebajas se completaron
        if (rebajas.all { it.completado }) {
            return EstadoVisual(
                "Rebajas completas",
                TipoEstadoVisual.COMPLETADO
            )
        }

        // Mostrar el último seguimiento realizado
        val ultimaCompletada = rebajas
            .lastOrNull { it.completado }

        if (ultimaCompletada != null) {
            return EstadoVisual(
                "${ultimaCompletada.porcentaje}% completado",
                TipoEstadoVisual.COMPLETADO
            )
        }

        // Comprobar si se acerca el vencimiento
        val dias = ChronoUnit.DAYS.between(
            hoy, vencimiento
        )

        if (dias <= 15) {
            return EstadoVisual(
                "Próximo a vencer",
                TipoEstadoVisual.PROXIMO_VENCER
            )
        }

        return EstadoVisual(
            "Vigente",
            TipoEstadoVisual.VIGENTE
        )
    }
}
