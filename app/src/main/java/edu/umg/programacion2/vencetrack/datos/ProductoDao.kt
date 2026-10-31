
package edu.umg.programacion2.vencetrack.datos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {

    // Guardar un producto
    @Insert
    suspend fun insertar(producto: ProductoEntity): Long

    // Obtener todos los productos
    @Query("SELECT * FROM productos ORDER BY fechaVencimiento ASC")
    fun obtenerProductos(): Flow<List<ProductoEntity>>

    // Buscar un producto por su ID
    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun obtenerPorId(id: Long): ProductoEntity?

    // Actualizar un producto
    @Update
    suspend fun actualizar(producto: ProductoEntity)

    // Eliminar un producto
    @Delete
    suspend fun eliminar(producto: ProductoEntity)
}
