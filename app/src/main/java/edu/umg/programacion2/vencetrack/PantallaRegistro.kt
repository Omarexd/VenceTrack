
package edu.umg.programacion2.vencetrack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import edu.umg.programacion2.vencetrack.logica.ValidadorProducto
import edu.umg.programacion2.vencetrack.modelo.Producto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import edu.umg.programacion2.vencetrack.modelo.Seguimiento
import edu.umg.programacion2.vencetrack.modelo.TipoSeguimiento
import edu.umg.programacion2.vencetrack.modelo.RotacionProducto
import androidx.compose.ui.platform.LocalContext
import edu.umg.programacion2.vencetrack.datos.ProductoEntity
import edu.umg.programacion2.vencetrack.datos.VenceTrackDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRegistro(onVolver: () -> Unit) {

    var alterno by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var fechaVencimiento by remember {

        mutableStateOf<LocalDate?>(null)
    }

    var fecha25 by remember { mutableStateOf<LocalDate?>(null) }
    var fecha50 by remember { mutableStateOf<LocalDate?>(null) }
    var fecha75 by remember { mutableStateOf<LocalDate?>(null) }

    var seguimientoSeleccionado by remember {
        mutableStateOf<TipoSeguimiento?>(null)
    }

    var rotacion by remember {
        mutableStateOf(RotacionProducto.BAJA)
    }

    fun calcularFechas(
        vencimiento: LocalDate,
        movimiento: RotacionProducto
    ) {
        fecha25 = vencimiento.minusDays(movimiento.diasAntes25)
        fecha50 = vencimiento.minusDays(movimiento.diasAntes50)
        fecha75 = vencimiento.minusDays(movimiento.diasAntes75)
    }

    var mostrarCalendario by remember {
        mutableStateOf(false)
    }

    var mensaje by remember { mutableStateOf("") }
    var hayErrores by remember { mutableStateOf(false) }

    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()

    val productoDao = remember(contexto) {
        VenceTrackDatabase
            .obtenerBaseDatos(contexto)
            .productoDao()
    }

    var guardando by remember { mutableStateOf(false) }
    BackHandler {
        onVolver()
    }

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
                Text("← Volver al inicio")
            }

            Text(
                text = "Registrar producto",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Ingresa los datos del producto y su fecha de vencimiento.",
                color = colores.onSurfaceVariant
            )


            OutlinedTextField(
                value = alterno,
                onValueChange = { valor ->
                    alterno = valor.filter { it.isDigit() }
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
                label = { Text("Descripción del producto") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cantidad,
                onValueChange = { valor ->
                    cantidad = valor.filter { it.isDigit() }
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
                    seguimientoSeleccionado = null
                    mostrarCalendario = true
                },

                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    fechaVencimiento?.format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    ) ?: "Seleccionar fecha"
                )
            }

            Text(
                text = "Movimiento de venta",
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Selecciona qué tan rápido se vende el producto.",
                color = colores.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                listOf(
                    "Poco movimiento" to RotacionProducto.BAJA,
                    "Mucho movimiento" to RotacionProducto.ALTA
                ).forEach { (nombre, opcion) ->

                    FilterChip(
                        selected = rotacion == opcion,
                        onClick = {
                            rotacion = opcion

                            fechaVencimiento?.let { fecha ->
                                calcularFechas(fecha, opcion)
                            }
                        },
                        label = {
                            Text(
                                text = nombre,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                text = "Fechas de seguimiento",
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Selecciona cuándo corresponde cada rebaja.",
                color = colores.onSurfaceVariant
            )

            listOf(
                TipoSeguimiento.PRIMERO to fecha25,
                TipoSeguimiento.SEGUNDO to fecha50,
                TipoSeguimiento.TERCERO to fecha75
            ).forEach { (tipo, fecha) ->

                OutlinedButton(
                    onClick = {
                        seguimientoSeleccionado = tipo
                        mostrarCalendario = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val fechaTexto = fecha?.format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    ) ?: "Seleccionar fecha"

                    Text("${tipo.descuento}% - $fechaTexto")
                }
            }


            Button(
                onClick = {

                    val fecha = fechaVencimiento

                    if (fecha == null) {
                        mensaje = "Selecciona la fecha de vencimiento."
                        hayErrores = true
                    } else {

                        val seguimientos = listOfNotNull(
                            fecha25?.let {
                                Seguimiento(TipoSeguimiento.PRIMERO, it)
                            },
                            fecha50?.let {
                                Seguimiento(TipoSeguimiento.SEGUNDO, it)
                            },
                            fecha75?.let {
                                Seguimiento(TipoSeguimiento.TERCERO, it)
                            }
                        )

                        val producto = Producto(
                            alterno = alterno.trim(),
                            descripcion = descripcion.trim(),
                            cantidad = cantidad.toIntOrNull() ?: 0,
                            fechaVencimiento = fecha,
                            seguimientos = seguimientos
                        )

                        val errores = ValidadorProducto.validar(producto)

                        if (errores.isNotEmpty()) {

                            hayErrores = true
                            mensaje = errores.joinToString("\n")

                        } else {

                            guardando = true
                            mensaje = ""

                            val registro = ProductoEntity(
                                alterno = producto.alterno,
                                descripcion = producto.descripcion,
                                cantidad = producto.cantidad,
                                fechaVencimiento = fecha.toString(),
                                rotacion = rotacion.name,

                                fecha25 = fecha25?.toString(),
                                fecha50 = fecha50?.toString(),
                                fecha75 = fecha75?.toString()
                            )

                            scope.launch {

                                try {


                                    withContext(Dispatchers.IO) {
                                        productoDao.insertar(registro)
                                    }

                                    // Limpiar el formulario
                                    alterno = ""
                                    descripcion = ""
                                    cantidad = ""

                                    fechaVencimiento = null
                                    fecha25 = null
                                    fecha50 = null
                                    fecha75 = null

                                    rotacion = RotacionProducto.BAJA
                                    seguimientoSeleccionado = null

                                    hayErrores = false
                                    mensaje = "Producto guardado. Puedes registrar el siguiente."

                                } catch (e: Exception) {

                                    hayErrores = true
                                    mensaje = "Error al guardar: ${e.message}"

                                } finally {
                                    guardando = false
                                }
                            }
                        }
                    }
                },
                enabled = !guardando,
                modifier = Modifier.fillMaxWidth()
            ) {


                Text(
                    if (guardando) "Guardando..."
                    else "Guardar producto"
                )

            }

            if (mensaje.isNotEmpty()) {
                Text(
                    text = mensaje,
                    color = if (hayErrores) {
                        colores.error
                    } else {
                        colores.primary
                    }
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

                            val fechaElegida = Instant
                                .ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()

                            when (seguimientoSeleccionado) {

                                null -> {
                                    fechaVencimiento = fechaElegida
                                    calcularFechas(fechaElegida, rotacion)
                                }

                                TipoSeguimiento.PRIMERO ->
                                    fecha25 = fechaElegida

                                TipoSeguimiento.SEGUNDO ->
                                    fecha50 = fechaElegida

                                TipoSeguimiento.TERCERO ->
                                    fecha75 = fechaElegida
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
                    onClick = { mostrarCalendario = false }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = calendario)
        }
    }
}
