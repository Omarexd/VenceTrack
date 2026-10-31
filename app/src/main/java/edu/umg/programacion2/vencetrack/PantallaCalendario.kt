
package edu.umg.programacion2.vencetrack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import edu.umg.programacion2.vencetrack.datos.*
import edu.umg.programacion2.vencetrack.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private data class EventoCalendario(
    val productoId: Long,
    val nombre: String,
    val tipo: String,
    val fecha: LocalDate,
    val esVencimiento: Boolean = false,
    val completado: Boolean = false,
    val omitido: Boolean = false
)

@Composable
fun PantallaCalendario(
    onVolver: () -> Unit,
    onVerDetalle: (Long) -> Unit
) {
    val contexto = LocalContext.current
    val colores = MaterialTheme.colorScheme

    val dao = remember(contexto) {
        VenceTrackDatabase
            .obtenerBaseDatos(contexto)
            .productoDao()
    }

    val flujo = remember(dao) {
        dao.obtenerProductos()
    }

    val productos by flujo.collectAsState(
        initial = emptyList<ProductoEntity>()
    )

    var mesActual by remember {
        mutableStateOf(YearMonth.now())
    }

    var diaSeleccionado by remember {
        mutableStateOf(LocalDate.now())
    }

    val eventos = productos.flatMap { producto ->

        buildList<EventoCalendario> {

            add(
                EventoCalendario(
                    producto.id,
                    producto.descripcion,
                    "Vencimiento",
                    LocalDate.parse(producto.fechaVencimiento),
                    esVencimiento = true
                )
            )

            producto.fecha25?.let {
                add(
                    EventoCalendario(
                        producto.id,
                        producto.descripcion,
                        "Rebaja 25 %",
                        LocalDate.parse(it),
                        completado = producto.completado25,
                        omitido = producto.omitido25
                    )
                )
            }

            producto.fecha50?.let {
                add(
                    EventoCalendario(
                        producto.id,
                        producto.descripcion,
                        "Rebaja 50 %",
                        LocalDate.parse(it),
                        completado = producto.completado50,
                        omitido = producto.omitido50
                    )
                )
            }

            producto.fecha75?.let {
                add(
                    EventoCalendario(
                        producto.id,
                        producto.descripcion,
                        "Rebaja 75 %",
                        LocalDate.parse(it),
                        completado = producto.completado75,
                        omitido = producto.omitido75
                    )
                )
            }
        }
    }

    val idioma = remember {
        Locale.forLanguageTag("es-GT")
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            Text(
                text = "Calendario",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colores.surface
                ),
                shape = RoundedCornerShape(18.dp)
            ) {

                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        TextButton(
                            onClick = {
                                mesActual = mesActual.minusMonths(1)
                                diaSeleccionado = mesActual.atDay(1)
                            }
                        ) {
                            Text("‹", fontSize = 26.sp)
                        }

                        Text(
                            text = mesActual.month
                                .getDisplayName(
                                    TextStyle.FULL,
                                    idioma
                                )
                                .replaceFirstChar {
                                    it.uppercase()
                                } + " ${mesActual.year}",
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = {
                                mesActual = mesActual.plusMonths(1)
                                diaSeleccionado = mesActual.atDay(1)
                            }
                        ) {
                            Text("›", fontSize = 26.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            "L", "M", "M", "J", "V", "S", "D"
                        ).forEach { dia ->

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dia,
                                    color = colores.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val desplazamiento =
                        mesActual.atDay(1).dayOfWeek.value - 1

                    val totalDias = mesActual.lengthOfMonth()

                    val semanas =
                        (desplazamiento + totalDias + 6) / 7

                    repeat(semanas) { semana ->

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(3.dp)
                        ) {

                            repeat(7) { columna ->

                                val numero =
                                    semana * 7 + columna -
                                            desplazamiento + 1

                                if (numero in 1..totalDias) {

                                    val fecha =
                                        mesActual.atDay(numero)

                                    val eventosDia = eventos.filter {
                                        it.fecha == fecha
                                    }

                                    CeldaCalendario(
                                        numero = numero,
                                        seleccionado =
                                            fecha == diaSeleccionado,
                                        esHoy =
                                            fecha == LocalDate.now(),
                                        eventos = eventosDia,
                                        onClick = {
                                            diaSeleccionado = fecha
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                } else {

                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "Eventos del ${
                    diaSeleccionado.dayOfMonth
                } de ${
                    diaSeleccionado.month.getDisplayName(
                        TextStyle.FULL, idioma
                    )
                }",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val eventosSeleccionados = eventos.filter {
                it.fecha == diaSeleccionado
            }

            if (eventosSeleccionados.isEmpty()) {

                Text(
                    text = "No hay vencimientos ni rebajas " +
                            "programadas para este día.",
                    color = colores.onSurfaceVariant
                )

            } else {

                eventosSeleccionados.forEach { evento ->

                    TarjetaEvento(
                        evento = evento,
                        onClick = {
                            onVerDetalle(evento.productoId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CeldaCalendario(
    numero: Int,
    seleccionado: Boolean,
    esHoy: Boolean,
    eventos: List<EventoCalendario>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colores = MaterialTheme.colorScheme

    val colorFondo = if (seleccionado) {
        colores.primaryContainer
    } else {
        Color.Transparent
    }

    Column(
        modifier = modifier
            .height(52.dp)
            .background(
                colorFondo,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = numero.toString(),
            color = if (seleccionado || esHoy) {
                colores.primary
            } else {
                colores.onSurface
            },
            fontWeight = if (seleccionado || esHoy) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {

            if (eventos.any { it.esVencimiento }) {
                PuntoCalendario(EstadoVencido)
            }

            if (eventos.any {
                    !it.esVencimiento &&
                            !it.completado &&
                            !it.omitido
                }) {
                PuntoCalendario(EstadoProximo)
            }

            if (eventos.any {
                    !it.esVencimiento && it.completado
                }) {
                PuntoCalendario(EstadoCompletado)
            }
        }
    }
}

@Composable
private fun PuntoCalendario(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(color, RoundedCornerShape(50))
    )
}

@Composable
private fun TarjetaEvento(
    evento: EventoCalendario,
    onClick: () -> Unit
) {
    val colores = MaterialTheme.colorScheme
    val hoy = LocalDate.now()

    val estado = when {
        evento.esVencimiento &&
                evento.fecha.isBefore(hoy) -> "Vencido"

        evento.esVencimiento &&
                evento.fecha == hoy -> "Vence hoy"

        evento.esVencimiento -> "Programado"

        evento.completado -> "Completado"
        evento.omitido -> "Omitido"
        evento.fecha.isBefore(hoy) -> "Atrasado"
        evento.fecha == hoy -> "Para hoy"
        else -> "Pendiente"
    }

    val colorEstado = when (estado) {
        "Vencido", "Atrasado" -> EstadoVencido
        "Completado" -> EstadoCompletado
        "Para hoy" -> EstadoProximo
        else -> colores.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = colores.surface
        ),
        border = BorderStroke(
            1.dp,
            colores.outline.copy(alpha = 0.5f)
        )
    ) {

        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = evento.nombre,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = evento.tipo,
                color = colores.onSurfaceVariant
            )

            Text(
                text = estado,
                color = colorEstado,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Toca para ver el producto",
                fontSize = 11.sp,
                color = colores.onSurfaceVariant
            )
        }
    }
}
