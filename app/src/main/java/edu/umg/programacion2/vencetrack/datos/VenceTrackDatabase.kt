
package edu.umg.programacion2.vencetrack.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProductoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VenceTrackDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao

    companion object {

        @Volatile
        private var INSTANCIA: VenceTrackDatabase? = null

        fun obtenerBaseDatos(
            context: Context
        ): VenceTrackDatabase {

            return INSTANCIA ?: synchronized(this) {

                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    VenceTrackDatabase::class.java,
                    "vencetrack_database"
                ).build()

                INSTANCIA = instancia
                instancia
            }
        }
    }
}
