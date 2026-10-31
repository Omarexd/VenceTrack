
package edu.umg.programacion2.vencetrack.logica

import edu.umg.programacion2.vencetrack.modelo.Producto

object ValidadorProducto {

    fun validar(producto: Producto): List<String> {

        val errores = mutableListOf<String>()

        if (producto.alterno.isBlank()) {
            errores.add("El alterno es obligatorio.")
        }

        if (producto.descripcion.isBlank()) {
            errores.add("La descripcion es obligatoria.")
        }

        if (producto.cantidad <= 0) {
            errores.add("La cantidad debe ser mayor que cero.")
        }

        val seguimientos = producto.seguimientos

        if (seguimientos.map { it.tipo }.distinct().size != seguimientos.size) {
            errores.add("No puede haber seguimientos repetidos.")
        }

        if (seguimientos.any {
                it.fechaProgramada.isAfter(producto.fechaVencimiento)
            }) {
            errores.add("Los seguimientos no pueden ser posteriores al vencimiento.")
        }

        val ordenados = seguimientos.sortedBy { it.tipo.ordinal }

        if (ordenados.zipWithNext().any { (primero, segundo) ->
                primero.fechaProgramada.isAfter(segundo.fechaProgramada)
            }) {
            errores.add("Las fechas de seguimiento deben estar en orden.")
        }

        return errores
    }
}
