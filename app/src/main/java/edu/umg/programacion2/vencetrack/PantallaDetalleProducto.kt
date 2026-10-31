
package edu.umg.programacion2.vencetrack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.umg.programacion2.vencetrack.datos.ProductoEntity
import edu.umg.programacion2.vencetrack.datos.VenceTrackDatabase
import edu.umg.programacion2.vencetrack.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable

fun PantallaDetalleProducto(
    productoId: Long,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
)

 {
    val contexto = LocalContext.current
    val colores = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val dao = remember(contexto) {
        VenceTrackDatabase
            .obtenerBaseDatos(contexto)
            .productoDao()
    }

    var producto by remember(productoId) {
        mutableStateOf<ProductoEntity?>(null)
    }

    var cargando by remember(productoId) {
        mutableStateOf(true)
    }

    var guardando by remember {
        mutableStateOf(false)
    }

     var mostrarDialogoEliminar by remember {
         mutableStateOf(false)
     }

     var eliminando by remember {
         mutableStateOf(false)
     }

     var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(productoId) {
        try {
            producto = withContext(Dispatchers.IO) {
                dao.obtenerPorId(productoId)
            }
        } catch (e: Exception) {
            error = "No se pudo cargar el producto."
        } finally {
            cargando = false
        }
    }


     fun alternarSeguimiento(porcentaje: Int) {
         val actual = producto ?: return

         if (guardando) return

         val nuevo25 = if (porcentaje == 25) {
             !actual.completado25
         } else {
             actual.completado25
         }

         val nuevo50 = if (porcentaje == 50) {
             !actual.completado50
         } else {
             actual.completado50
         }

         val nuevo75 = if (porcentaje == 75) {
             !actual.completado75
         } else {
             actual.completado75
         }

         val actualizado = actual.copy(
             completado25 = nuevo25,
             completado50 = nuevo50,
             completado75 = nuevo75,

             omitido25 = !nuevo25 && (nuevo50 || nuevo75),
             omitido50 = !nuevo50 && nuevo75,
             omitido75 = false
         )

         scope.launch {
             guardando = true
             error = null

             try {
                 withContext(Dispatchers.IO) {
                     dao.actualizar(actualizado)
                 }

                 producto = actualizado

             } catch (e: Exception) {
                 error = "No se pudo actualizar el seguimiento."
             } finally {
                 guardando = false
             }
         }
     }

     BackHandler(onBack = onVolver)

    Scaffold(
        containerColor = colores.background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            TextButton(onClick = onVolver) {
                Text("← Volver")
            }

            Text(
                text = "Detalle del producto",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (cargando) {
                CircularProgressIndicator()
            } else {
                val registro = producto

                if (registro == null) {
                    Text("No se encontró el producto.")
                } else {

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = registro.descripcion,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text("Alterno: ${registro.alterno}")
                            Text("Cantidad: ${registro.cantidad}")

                            Text(
                                text = "Vence: ${
                                    formatearFecha(registro.fechaVencimiento)
                                }",
                                color = colores.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onEditar,
                        enabled = !guardando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Editar producto")
                    }

                    Text(

                        text = "Seguimientos de rebajas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    FilaSeguimiento(
                        porcentaje = 25,
                        fecha = registro.fecha25,
                        completado = registro.completado25,
                        omitido = registro.omitido25,
                        guardando = guardando,
                        onConfirmar = {
                            alternarSeguimiento(25)
                        }
                    )

                    FilaSeguimiento(
                        porcentaje = 50,
                        fecha = registro.fecha50,
                        completado = registro.completado50,
                        omitido = registro.omitido50,
                        guardando = guardando,
                        onConfirmar = {
                            alternarSeguimiento(50)
                        }
                    )

                    FilaSeguimiento(
                        porcentaje = 75,
                        fecha = registro.fecha75,
                        completado = registro.completado75,
                        omitido = registro.omitido75,
                        guardando = guardando,
                        onConfirmar = {
                            alternarSeguimiento(75)
                        }
                    )

                    OutlinedButton(
                        onClick = {
                            mostrarDialogoEliminar = true
                        },
                        enabled = !guardando && !eliminando,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colores.error
                        )
                    ) {
                        Text("Eliminar producto")
                    }
                }
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = colores.error
                )
            }
        }
    }

     if (mostrarDialogoEliminar) {

         AlertDialog(
             onDismissRequest = {
                 if (!eliminando) {
                     mostrarDialogoEliminar = false
                 }
             },

             title = {
                 Text("Eliminar producto")
             },

             text = {
                 Text(
                     "¿Estás seguro de que deseas eliminar " +
                             "este producto? También se eliminarán " +
                             "sus seguimientos. Esta acción no se puede deshacer."
                 )
             },

             confirmButton = {
                 TextButton(
                     enabled = !eliminando,
                     onClick = {
                         val actual = producto

                         if (actual != null) {
                             scope.launch {
                                 eliminando = true
                                 error = null

                                 try {
                                     withContext(Dispatchers.IO) {
                                         dao.eliminar(actual)
                                     }

                                     mostrarDialogoEliminar = false
                                     onEliminar()

                                 } catch (e: Exception) {
                                     error = "No se pudo eliminar el producto."
                                     mostrarDialogoEliminar = false
                                 } finally {
                                     eliminando = false
                                 }
                             }
                         }
                     }
                 ) {
                     Text(
                         if (eliminando) "Eliminando..."
                         else "Eliminar",
                         color = colores.error
                     )
                 }
             },

             dismissButton = {
                 TextButton(
                     enabled = !eliminando,
                     onClick = {
                         mostrarDialogoEliminar = false
                     }
                 ) {
                     Text("Cancelar")
                 }
             }
         )
     }
 }

@Composable
private fun FilaSeguimiento(
    porcentaje: Int,
    fecha: String?,
    completado: Boolean,
    omitido: Boolean,
    guardando: Boolean,
    onConfirmar: () -> Unit
) {
    val colores = MaterialTheme.colorScheme

    val fechaReal = fecha?.let {
        LocalDate.parse(it)
    }

    val estado = when {
        completado -> "Completado"
        omitido -> "Omitido"
        fechaReal == null -> "Sin programar"
        fechaReal.isBefore(LocalDate.now()) -> "Atrasado"
        fechaReal.isEqual(LocalDate.now()) -> "Para hoy"
        else -> "Pendiente"
    }

    val colorEstado = when (estado) {
        "Completado" -> EstadoCompletado
        "Atrasado" -> EstadoVencido
        "Para hoy" -> EstadoProximo
        else -> colores.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = "Seguimiento $porcentaje%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Fecha: ${formatearFecha(fecha)}",
                color = colores.onSurfaceVariant
            )

            Text(
                text = estado,
                color = colorEstado,
                fontWeight = FontWeight.SemiBold
            )

            if (fecha != null) {
                FilterChip(
                    selected = completado,
                    onClick = onConfirmar,
                    enabled = !guardando,
                    label = {
                        Text("Rebaja del $porcentaje%")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EstadoCompletado.copy(
                            alpha = 0.18f
                        ),
                        selectedLabelColor = EstadoCompletado
                    )
                )
            }
        }
    }
}

private fun formatearFecha(fecha: String?): String {
    if (fecha == null) return "Sin programar"

    return LocalDate.parse(fecha).format(
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    )
}
