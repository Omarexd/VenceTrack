
package edu.umg.programacion2.vencetrack.logica

import edu.umg.programacion2.vencetrack.modelo.*
import java.time.LocalDate

object GestorSeguimientos {

    fun obtenerEstado(
        seguimiento: Seguimiento,
        hoy: LocalDate = LocalDate.now()
    ): EstadoSeguimiento {

        if (seguimiento.completado) {
            return EstadoSeguimiento.COMPLETADO
        }

        if (seguimiento.omitido) {
            return EstadoSeguimiento.OMITIDO
        }

        val dias = CalculadoraFechas.diasRestantes(
            seguimiento.fechaProgramada, hoy
        )

        return when {
            dias < 0 -> EstadoSeguimiento.ATRASADO
            dias == 0L -> EstadoSeguimiento.PARA_HOY
            else -> EstadoSeguimiento.PENDIENTE
        }
    }

    fun completarSeguimiento(
        producto: Producto,
        tipo: TipoSeguimiento
    ): Producto {

        val seguimientosActualizados =
            producto.seguimientos.map { seguimiento ->

                when {
                    seguimiento.tipo == tipo ->
                        seguimiento.copy(
                            completado = true,
                            omitido = false
                        )

                    seguimiento.tipo.ordinal < tipo.ordinal &&
                            !seguimiento.completado ->
                        seguimiento.copy(omitido = true)

                    else -> seguimiento
                }
            }

        return producto.copy(
            seguimientos = seguimientosActualizados
        )
    }
}
