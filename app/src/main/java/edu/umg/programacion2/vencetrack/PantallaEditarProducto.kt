
package edu.umg.programacion2.vencetrack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import edu.umg.programacion2.vencetrack.datos.*
import edu.umg.programacion2.vencetrack.modelo.*
import edu.umg.programacion2.vencetrack.logica.ValidadorProducto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditarProducto(
    productoId: Long,
    onVolver: () -> Unit
) {
    val contexto = LocalContext.current
    val colores = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val dao = remember(contexto) {
        VenceTrackDatabase
            .obtenerBaseDatos(contexto)
            .productoDao()
    }

    var original by remember(productoId) {
        mutableStateOf<ProductoEntity?>(null)
    }

    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    var alterno by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }

    var vencimiento by remember {
        mutableStateOf<LocalDate?>(null)
    }

    var fecha25 by remember {
        mutableStateOf<LocalDate?>(null)
    }

    var fecha50 by remember {
        mutableStateOf<LocalDate?>(null)
    }

    var fecha75 by remember {
        mutableStateOf<LocalDate?>(null)
    }

    var rotacion by remember {
        mutableStateOf(RotacionProducto.BAJA)
    }

    var mostrarCalendario by remember {
        mutableStateOf(false)
    }

    // 0 = vencimiento, 25/50/75 = seguimiento
    var campoFecha by remember { mutableIntStateOf(0) }

    val formato = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    fun recalcularFechas(fecha: LocalDate) {
        fecha25 = fecha.minusDays(rotacion.diasAntes25)
        fecha50 = fecha.minusDays(rotacion.diasAntes50)
        fecha75 = fecha.minusDays(rotacion.diasAntes75)
    }

    LaunchedEffect(productoId) {
        try {
            val registro = withContext(Dispatchers.IO) {
                dao.obtenerPorId(productoId)
            }

            if (registro != null) {
                original = registro

                alterno = registro.alterno
                descripcion = registro.descripcion
                cantidad = registro.cantidad.toString()

                vencimiento = LocalDate.parse(
                    registro.fechaVencimiento
                )

                fecha25 = registro.fecha25?.let {
                    LocalDate.parse(it)
                }

                fecha50 = registro.fecha50?.let {
                    LocalDate.parse(it)
                }

                fecha75 = registro.fecha75?.let {
                    LocalDate.parse(it)
                }

                rotacion = runCatching {
                    RotacionProducto.valueOf(registro.rotacion)
                }.getOrDefault(RotacionProducto.BAJA)
            } else {
                mensaje = "No se encontró el producto."
            }
        } catch (e: Exception) {
            mensaje = "No se pudo cargar el producto."
        } finally {
            cargando = false
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
                text = "Editar producto",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (cargando) {
                CircularProgressIndicator()
            } else if (original != null) {

                OutlinedTextField(
                    value = alterno,
                    onValueChange = {
                        alterno = it.filter { c -> c.isDigit() }
                    },
                    label = { Text("Código alterno") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cantidad,
                    onValueChange = {
                        cantidad = it.filter { c -> c.isDigit() }
                    },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Fecha de vencimiento",
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedButton(
                    onClick = {
                        campoFecha = 0
                        mostrarCalendario = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        vencimiento?.format(formato)
                            ?: "Seleccionar fecha"
                    )
                }

                Text(
                    text = "Movimiento de venta",
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Poco movimiento" to RotacionProducto.BAJA,
                        "Mucho movimiento" to RotacionProducto.ALTA
                    ).forEach { (nombre, tipo) ->

                        FilterChip(
                            selected = rotacion == tipo,
                            onClick = {
                                rotacion = tipo
                                vencimiento?.let {
                                    recalcularFechas(it)
                                }
                            },
                            label = { Text(nombre, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = "Fechas de seguimiento",
                    fontWeight = FontWeight.SemiBold
                )

                listOf(
                    25 to fecha25,
                    50 to fecha50,
                    75 to fecha75
                ).forEach { (porcentaje, fecha) ->

                    OutlinedButton(
                        onClick = {
                            campoFecha = porcentaje
                            mostrarCalendario = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "$porcentaje% - " +
                                    (fecha?.format(formato)
                                        ?: "Sin programar")
                        )
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !guardando,
                    onClick = {
                        val actual = original
                        val fecha = vencimiento

                        if (actual == null || fecha == null) {
                            mensaje = "Selecciona el vencimiento."
                        } else {

                            val seguimientos = listOfNotNull(
                                fecha25?.let {
                                    Seguimiento(
                                        TipoSeguimiento.PRIMERO, it
                                    )
                                },
                                fecha50?.let {
                                    Seguimiento(
                                        TipoSeguimiento.SEGUNDO, it
                                    )
                                },
                                fecha75?.let {
                                    Seguimiento(
                                        TipoSeguimiento.TERCERO, it
                                    )
                                }
                            )

                            val producto = Producto(
                                alterno = alterno.trim(),
                                descripcion = descripcion.trim(),
                                cantidad = cantidad.toIntOrNull() ?: 0,
                                fechaVencimiento = fecha,
                                seguimientos = seguimientos
                            )

                            val errores =
                                ValidadorProducto.validar(producto)

                            if (errores.isNotEmpty()) {
                                mensaje = errores.joinToString("\n")
                            } else {

                                val f25 = fecha25?.toString()
                                val f50 = fecha50?.toString()
                                val f75 = fecha75?.toString()

                                val actualizado = actual.copy(
                                    alterno = producto.alterno,
                                    descripcion = producto.descripcion,
                                    cantidad = producto.cantidad,
                                    fechaVencimiento = fecha.toString(),
                                    rotacion = rotacion.name,
                                    fecha25 = f25,
                                    fecha50 = f50,
                                    fecha75 = f75,

                                    completado25 =
                                        if (f25 == actual.fecha25)
                                            actual.completado25 else false,
                                    completado50 =
                                        if (f50 == actual.fecha50)
                                            actual.completado50 else false,
                                    completado75 =
                                        if (f75 == actual.fecha75)
                                            actual.completado75 else false,

                                    omitido25 =
                                        if (f25 == actual.fecha25)
                                            actual.omitido25 else false,
                                    omitido50 =
                                        if (f50 == actual.fecha50)
                                            actual.omitido50 else false,
                                    omitido75 =
                                        if (f75 == actual.fecha75)
                                            actual.omitido75 else false
                                )

                                guardando = true
                                mensaje = ""

                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            dao.actualizar(actualizado)
                                        }
                                        onVolver()
                                    } catch (e: Exception) {
                                        mensaje = "Error al actualizar."
                                    } finally {
                                        guardando = false
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        if (guardando) "Guardando..."
                        else "Guardar cambios"
                    )
                }
            }

            if (mensaje.isNotBlank()) {
                Text(
                    text = mensaje,
                    color = colores.error
                )
            }
        }
    }

    if (mostrarCalendario) {
        val calendario = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = {
                mostrarCalendario = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        calendario.selectedDateMillis?.let { millis ->

                            val fecha = Instant
                                .ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()

                            when (campoFecha) {
                                0 -> {
                                    vencimiento = fecha
                                    recalcularFechas(fecha)
                                }
                                25 -> fecha25 = fecha
                                50 -> fecha50 = fecha
                                75 -> fecha75 = fecha
                            }
                        }

                        mostrarCalendario = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarCalendario = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = calendario)
        }
    }
}
