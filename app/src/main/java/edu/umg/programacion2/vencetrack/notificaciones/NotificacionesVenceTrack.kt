
package edu.umg.programacion2.vencetrack.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import edu.umg.programacion2.vencetrack.MainActivity
import edu.umg.programacion2.vencetrack.R
import edu.umg.programacion2.vencetrack.datos.VenceTrackDatabase
import edu.umg.programacion2.vencetrack.logica.EstadoVisualProducto
import edu.umg.programacion2.vencetrack.logica.TipoEstadoVisual
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

object ProgramadorNotificaciones {

    fun programar(contexto: Context) {

        val solicitud =
            PeriodicWorkRequestBuilder<RevisionAlertasWorker>(
                24, TimeUnit.HOURS
            ).build()

        WorkManager.getInstance(contexto)
            .enqueueUniquePeriodicWork(
                "revision_diaria_vencetrack",
                ExistingPeriodicWorkPolicy.KEEP,
                solicitud
            )
    }
}

class RevisionAlertasWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {

        return try {

            val hoy = LocalDate.now()
            val contexto = applicationContext

            val preferencias = contexto.getSharedPreferences(
                "avisos_vencetrack",
                Context.MODE_PRIVATE
            )

            // Evitar varias notificaciones el mismo día
            if (
                preferencias.getString("ultima_fecha", null)
                == hoy.toString()
            ) {
                return Result.success()
            }

            val productos = VenceTrackDatabase
                .obtenerBaseDatos(contexto)
                .productoDao()
                .obtenerProductos()
                .first()

            val mensajes = productos.mapNotNull { producto ->

                val estado = EstadoVisualProducto.obtenerEstado(
                    producto, hoy
                )

                val requiereAtencion = when (estado.tipo) {
                    TipoEstadoVisual.VENCIDO,
                    TipoEstadoVisual.VENCE_HOY,
                    TipoEstadoVisual.REBAJA_ATRASADA,
                    TipoEstadoVisual.REBAJA_HOY,
                    TipoEstadoVisual.REBAJA_PROXIMA,
                    TipoEstadoVisual.PROXIMO_VENCER -> true

                    else -> false
                }

                if (requiereAtencion) {
                    "${producto.descripcion}: ${estado.texto}"
                } else {
                    null
                }
            }

            if (mensajes.isNotEmpty()) {

                val enviado = mostrarNotificacion(
                    contexto,
                    mensajes
                )

                if (enviado) {
                    preferencias.edit()
                        .putString(
                            "ultima_fecha",
                            hoy.toString()
                        )
                        .apply()
                }
            }

            Result.success()

        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun mostrarNotificacion(
        contexto: Context,
        mensajes: List<String>
    ): Boolean {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            val permiso = ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permiso != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }

        val administrador = contexto.getSystemService(
            NotificationManager::class.java
        )

        if (!administrador.areNotificationsEnabled()) {
            return false
        }

        val canalId = "alertas_vencetrack"

        val canal = NotificationChannel(
            canalId,
            "Alertas de vencimientos",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description =
                "Avisos de rebajas y fechas de vencimiento"
        }

        administrador.createNotificationChannel(canal)

        val intent = Intent(
            contexto,
            MainActivity::class.java
        )

        val abrirApp = PendingIntent.getActivity(
            contexto,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val detalles = mensajes.take(6).joinToString("\n")

        val titulo = if (mensajes.size == 1) {
            "VenceTrack: 1 producto por revisar"
        } else {
            "VenceTrack: ${mensajes.size} productos por revisar"
        }

        val notificacion = NotificationCompat.Builder(
            contexto,
            canalId
        )
            .setSmallIcon(R.drawable.ic_event)
            .setContentTitle(titulo)
            .setContentText(mensajes.first())
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(detalles)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        administrador.notify(1001, notificacion)

        return true
    }
}
