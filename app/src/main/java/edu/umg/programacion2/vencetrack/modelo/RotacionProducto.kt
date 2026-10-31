
package edu.umg.programacion2.vencetrack.modelo

enum class RotacionProducto(
    val diasAntes25: Long,
    val diasAntes50: Long,
    val diasAntes75: Long
) {
    BAJA(30, 20, 10),
    ALTA(15, 10, 5)
}
