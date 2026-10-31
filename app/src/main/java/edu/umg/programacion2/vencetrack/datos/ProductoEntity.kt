
package edu.umg.programacion2.vencetrack.datos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class ProductoEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val alterno: String,
    val descripcion: String,
    val cantidad: Int,
    val fechaVencimiento: String,

    val rotacion: String,

    val fecha25: String?,
    val fecha50: String?,
    val fecha75: String?,

    val completado25: Boolean = false,
    val completado50: Boolean = false,
    val completado75: Boolean = false,

    val omitido25: Boolean = false,
    val omitido50: Boolean = false,
    val omitido75: Boolean = false
)
