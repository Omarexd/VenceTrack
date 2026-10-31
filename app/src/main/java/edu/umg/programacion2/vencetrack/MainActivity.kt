
package edu.umg.programacion2.vencetrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.umg.programacion2.vencetrack.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import edu.umg.programacion2.vencetrack.datos.ProductoEntity
import edu.umg.programacion2.vencetrack.datos.VenceTrackDatabase
import androidx.compose.foundation.clickable
import edu.umg.programacion2.vencetrack.logica.EstadoVisual
import edu.umg.programacion2.vencetrack.logica.EstadoVisualProducto
import edu.umg.programacion2.vencetrack.logica.TipoEstadoVisual
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import edu.umg.programacion2.vencetrack.notificaciones.ProgramadorNotificaciones
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun ActivarNotificaciones() {

    val contexto = LocalContext.current

    val solicitarPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->

        if (concedido) {
            ProgramadorNotificaciones.programar(contexto)
        }
    }

    LaunchedEffect(Unit) {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            val permiso = ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permiso == PackageManager.PERMISSION_GRANTED) {
                ProgramadorNotificaciones.programar(contexto)
            } else {
                solicitarPermiso.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }

        } else {
            ProgramadorNotificaciones.programar(contexto)
        }
    }
}
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VenceTrackTheme {
                ActivarNotificaciones()
                var pestanaActual by rememberSaveable {
                    mutableStateOf("inicio")
                }

                var mostrarRegistro by remember {
                    mutableStateOf(false)
                }

                var mostrarEdicion by remember {
                    mutableStateOf(false)
                }

                var idProductoSeleccionado by remember {
                    mutableStateOf<Long?>(null)
                }

                when {

                    mostrarRegistro -> {
                        PantallaRegistro(
                            onVolver = {
                                mostrarRegistro = false
                            }
                        )
                    }

                    mostrarEdicion &&
                            idProductoSeleccionado != null -> {

                        PantallaEditarProducto(
                            productoId = idProductoSeleccionado!!,
                            onVolver = {
                                mostrarEdicion = false
                            }
                        )
                    }

                    idProductoSeleccionado != null -> {

                        PantallaDetalleProducto(
                            productoId = idProductoSeleccionado!!,

                            onVolver = {
                                idProductoSeleccionado = null
                            },

                            onEditar = {
                                mostrarEdicion = true
                            },

                            onEliminar = {
                                idProductoSeleccionado = null
                            }
                        )
                    }

                    else -> {

                        Scaffold(
                            containerColor =
                                MaterialTheme.colorScheme.background,

                            bottomBar = {
                                BarraNavegacion(
                                    pestanaActual = pestanaActual,
                                    onSeleccionar = { pestana ->
                                        pestanaActual = pestana
                                    }
                                )
                            }
                        ) { padding ->

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .consumeWindowInsets(padding)
                            ) {

                                if (pestanaActual == "inicio") {

                                    PantallaInicio(
                                        onRegistrar = {
                                            mostrarRegistro = true
                                        },

                                        onVerDetalle = { id ->
                                            idProductoSeleccionado = id
                                        }
                                    )

                                } else {

                                    PantallaCalendario(
                                        onVolver = {
                                            pestanaActual = "inicio"
                                        },

                                        onVerDetalle = { id ->
                                            idProductoSeleccionado = id
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ProductoDemo(
    val id: Long,
    val nombre: String,
    val alterno: String,
    val cantidad: Int,
    val fecha: LocalDate,
    val iniciales: String,
    val seguimientoCompletado: Boolean = false,
    val estadoVisual: EstadoVisual
)

@Composable

fun PantallaInicio(
    onRegistrar: () -> Unit,
    onVerDetalle: (Long) -> Unit
) {

    val colores = MaterialTheme.colorScheme
    val hoy = LocalDate.now()


    val contexto = LocalContext.current

    val dao = remember(contexto) {
        VenceTrackDatabase
            .obtenerBaseDatos(contexto)
            .productoDao()
    }

    val flujoProductos = remember(dao) {
        dao.obtenerProductos()
    }

    val registros by flujoProductos.collectAsState(
        initial = emptyList<ProductoEntity>()
    )

    val productos = registros.map { registro ->
        ProductoDemo(
            id = registro.id,
            nombre = registro.descripcion,
            alterno = registro.alterno,
            cantidad = registro.cantidad,
            fecha = LocalDate.parse(registro.fechaVencimiento),
            iniciales = registro.descripcion.trim()
                .take(2).uppercase(),
            seguimientoCompletado = registro.completado25,
            estadoVisual = EstadoVisualProducto.obtenerEstado(
                    registro, hoy
         )
        )
    }

    val vencidos = productos.count {
        it.fecha.isBefore(hoy)
    }

    val porRevisar = registros.count { registro ->

        val fecha = LocalDate.parse(registro.fechaVencimiento)

        val dias = ChronoUnit.DAYS.between(hoy, fecha)

        val seguimientoPendiente = listOf(
            registro.fecha25 to
                    (registro.completado25 || registro.omitido25),

            registro.fecha50 to
                    (registro.completado50 || registro.omitido50),

            registro.fecha75 to
                    (registro.completado75 || registro.omitido75)
        ).any { (fechaSeguimiento, finalizado) ->

            fechaSeguimiento != null &&
                    !finalizado &&
                    !LocalDate.parse(fechaSeguimiento).isAfter(hoy)
        }

        dias >= 0L && (
                dias <= 15L || seguimientoPendiente
                )
    }


    Scaffold(
        containerColor = colores.background
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = 18.dp,
                vertical = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            item {
                EncabezadoVenceTrack()
            }

            item {
                Button(
                    onClick = onRegistrar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Registrar producto")
                }
            }

            item {
                Text(
                    text = "Resumen general",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = colores.onBackground
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {


                    TarjetaResumen(
                        titulo = "Productos",
                        cantidad = productos.size.toString(),
                        icono = R.drawable.ic_inventory,
                        color = colores.primary,
                        modifier = Modifier.weight(1f)
                    )


                    TarjetaResumen(
                        titulo = "Por revisar",
                        cantidad = porRevisar.toString(),
                        icono = R.drawable.ic_schedule,
                        color = EstadoProximo,
                        modifier = Modifier.weight(1f)
                    )

                    TarjetaResumen(
                        titulo = "Vencidos",
                        cantidad = vencidos.toString(),
                        icono = R.drawable.ic_warning,
                        color = EstadoVencido,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "Vencimientos y seguimientos",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = colores.onBackground
                )
            }


            if (productos.isEmpty()) {

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colores.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Sin productos registrados",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Presiona Registrar producto para comenzar.",
                                color = colores.onSurfaceVariant
                            )
                        }
                    }
                }

            } else {

                items(productos) { producto ->
                    TarjetaProducto(
                        producto = producto,
                        onClick = {
                            onVerDetalle(producto.id)
                        }
                    )
                }
            }
        }
    }
}

// ENCABEZADO DE LA APLICACIÓN

@Composable
private fun EncabezadoVenceTrack() {

    val colores = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    colores.primary.copy(alpha = 0.13f),
                    RoundedCornerShape(15.dp)
                ),
            contentAlignment = Alignment.Center
        ) {


            Icon(
                painter = painterResource(R.drawable.ic_event),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = colores.primary
            )

        }

        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {

            Row {

                Text(
                    text = "Vence",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = colores.onBackground
                )

                Text(
                    text = "Track",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = colores.primary
                )
            }

            Text(
                text = "Control de fechas de vencimiento",
                fontSize = 12.sp,
                color = colores.onSurfaceVariant
            )
        }
    }
}

// TARJETAS DEL RESUMEN GENERAL

@Composable
fun TarjetaResumen(
    titulo: String,
    cantidad: String,
    icono: Int,
    color: Color,
    modifier: Modifier = Modifier
) {

    val oscuro = isSystemInDarkTheme()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(
                alpha = if (oscuro) 0.12f else 0.09f
            )
        ),
        border = BorderStroke(
            1.dp,
            color.copy(alpha = 0.35f)
        )
    ) {

        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {


            Icon(
                painter = painterResource(id = icono),
                contentDescription = null,
                modifier = Modifier.size(25.dp),
                tint = color
            )


            Text(
                text = titulo,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Text(
                text = cantidad,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// TARJETAS DE LOS PRODUCTOS

@Composable

private fun TarjetaProducto(
    producto: ProductoDemo,
    onClick: () -> Unit
) {

    val colores = MaterialTheme.colorScheme
    val oscuro = isSystemInDarkTheme()
    val dias = ChronoUnit.DAYS.between(
        LocalDate.now(),
        producto.fecha
    )

    val estado = producto.estadoVisual

    val textoEstado = estado.texto

    val colorEstado = when (estado.tipo) {

        TipoEstadoVisual.VENCIDO,
        TipoEstadoVisual.VENCE_HOY,
        TipoEstadoVisual.REBAJA_ATRASADA -> EstadoVencido

        TipoEstadoVisual.REBAJA_HOY,
        TipoEstadoVisual.REBAJA_PROXIMA,
        TipoEstadoVisual.PROXIMO_VENCER -> EstadoProximo

        TipoEstadoVisual.COMPLETADO,
        TipoEstadoVisual.VIGENTE -> EstadoCompletado
    }

    val mensajeDias = when {
        dias < 0 -> "Venció hace ${-dias} días"
        dias == 0L -> "Vence hoy"
        else -> "Faltan $dias días"
    }

    val fechaTexto = producto.fecha.format(
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    )

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colores.surface
        ),
        border = BorderStroke(
            1.dp,
            colores.outline.copy(alpha = 0.55f)
        )
    ) {

        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(51.dp)
                        .background(
                            colores.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = producto.iniciales,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colores.primary
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        text = producto.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colores.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Alterno: ${producto.alterno}",
                        fontSize = 12.sp,
                        color = colores.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                color = colores.outline.copy(alpha = 0.35f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {
                    Text(
                        text = "Cantidad",
                        fontSize = 11.sp,
                        color = colores.onSurfaceVariant
                    )

                    Text(
                        text = "${producto.cantidad} unidades",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colores.onSurface
                    )
                }

                Column {
                    Text(
                        text = "Vencimiento",
                        fontSize = 11.sp,
                        color = colores.onSurfaceVariant
                    )

                    Text(
                        text = fechaTexto,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colores.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    color = colorEstado.copy(
                        alpha = if (oscuro) 0.16f else 0.11f
                    ),
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(
                        1.dp,
                        colorEstado.copy(alpha = 0.45f)
                    )
                ) {

                    Text(
                        text = textoEstado,
                        color = colorEstado,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        )
                    )
                }

                Text(
                    text = mensajeDias,
                    color = colorEstado,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun BarraNavegacion(
    pestanaActual: String,
    onSeleccionar: (String) -> Unit
) {
    val colores = MaterialTheme.colorScheme

    val estilo = NavigationBarItemDefaults.colors(
        selectedIconColor = colores.primary,
        selectedTextColor = colores.primary,
        indicatorColor = colores.primary.copy(alpha = 0.15f),
        unselectedIconColor = colores.onSurfaceVariant,
        unselectedTextColor = colores.onSurfaceVariant
    )

    NavigationBar(
        containerColor = if (isSystemInDarkTheme()) {
            Color.Black
        } else {
            colores.surface
        },
        tonalElevation = 0.dp
    )
    {

        NavigationBarItem(
            selected = pestanaActual == "inicio",
            onClick = {
                onSeleccionar("inicio")
            },
            icon = {
                Icon(
                    painter = painterResource(
                        R.drawable.ic_home
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text("Inicio")
            },
            colors = estilo
        )

        NavigationBarItem(
            selected = pestanaActual == "calendario",
            onClick = {
                onSeleccionar("calendario")
            },
            icon = {
                Icon(
                    painter = painterResource(
                        R.drawable.ic_event
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text("Calendario")
            },
            colors = estilo
        )
    }
}
