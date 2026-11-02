package com.olius.app.presentation.screen.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.olius.app.R
import com.olius.app.domain.entity.CollectionPoint
import com.olius.app.domain.entity.CollectionPointType
import com.olius.app.domain.entity.MapBounds
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

/** Espera depois do último movimento antes de pedir os pins da nova área. */
private const val VIEWPORT_DELAY_MS = 400L
private const val PIN_SIZE_DP = 34
private const val PIN_SELECTED_SIZE_DP = 46
private const val USER_DOT_SIZE_DP = 16

/** Ícone (drawable) de cada tipo de ponto: os mesmos da legenda do mapa. */
val CollectionPointType.iconRes: Int
    get() = when (this) {
        CollectionPointType.PEV -> R.drawable.ponto_pev
        CollectionPointType.ESTABLISHMENT -> R.drawable.ponto_estabelecimento_voluntario
        CollectionPointType.COLLECTOR -> R.drawable.ponto_coletor_voluntario
    }

/** Mapa real com tiles do OpenStreetMap (osmdroid): sem API key e sem cartão. */
@Composable
fun OsmMapView(
    points: List<CollectionPoint>,
    selectedPointId: String?,
    userLocation: UserLocation?,
    cameraRequest: CameraRequest?,
    onPointClick: (String) -> Unit,
    onViewportChanged: (MapBounds) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnPointClick by rememberUpdatedState(onPointClick)
    val currentOnViewportChanged by rememberUpdatedState(onViewportChanged)
    val holder = remember { MapStateHolder() }

    val mapView = remember {
        configureOsmdroid(context)
        createMapView(context) { bounds -> currentOnViewportChanged(bounds) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            if (holder.points != points || holder.selectedId != selectedPointId || holder.userLocation != userLocation) {
                holder.points = points
                holder.selectedId = selectedPointId
                holder.userLocation = userLocation
                syncMarkers(map, points, selectedPointId, userLocation, holder) { id -> currentOnPointClick(id) }
            }
            if (cameraRequest != null && cameraRequest.id != holder.cameraRequestId) {
                holder.cameraRequestId = cameraRequest.id
                map.controller.animateTo(GeoPoint(cameraRequest.latitude, cameraRequest.longitude), cameraRequest.zoom, 600L)
            }
        }
    )
}

/** Guarda o que já foi desenhado, para não recriar os pins a cada recomposição. */
private class MapStateHolder {
    var points: List<CollectionPoint>? = null
    var selectedId: String? = null
    var userLocation: UserLocation? = null
    var cameraRequestId: Long? = null
    val icons = mutableMapOf<Pair<CollectionPointType, Boolean>, Drawable>()
    var userDot: Drawable? = null
}

/** O tile.openstreetmap.org BLOQUEIA o User-Agent padrão do osmdroid (o mapa fica cinza). */
private fun configureOsmdroid(context: Context) {
    val config = Configuration.getInstance()
    config.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    config.userAgentValue = context.packageName
    val base = File(context.cacheDir, "osmdroid")
    config.osmdroidBasePath = base
    config.osmdroidTileCache = File(base, "tiles")
}

@SuppressLint("ClickableViewAccessibility")
private fun createMapView(context: Context, onViewportChanged: (MapBounds) -> Unit): MapView =
    MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        minZoomLevel = 4.0
        maxZoomLevel = 19.0
        isTilesScaledToDpi = true
        controller.setZoom(DEFAULT_ZOOM)
        controller.setCenter(GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))

        setOnTouchListener { view, _ ->
            view.parent?.requestDisallowInterceptTouchEvent(true)
            false
        }

        fun report() {
            val box = boundingBox
            onViewportChanged(MapBounds(south = box.latSouth, west = box.lonWest, north = box.latNorth, east = box.lonEast))
        }
        addMapListener(DelayedMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean { report(); return false }
            override fun onZoom(event: ZoomEvent?): Boolean { report(); return false }
        }, VIEWPORT_DELAY_MS))
        addOnFirstLayoutListener { _, _, _, _, _ -> report() }
    }

private fun syncMarkers(
    map: MapView,
    points: List<CollectionPoint>,
    selectedId: String?,
    userLocation: UserLocation?,
    holder: MapStateHolder,
    onPointClick: (String) -> Unit
) {
    map.overlays.removeAll { it is Marker }

    userLocation?.let { location ->
        val dot = holder.userDot ?: userDotDrawable(map.context).also { holder.userDot = it }
        map.overlays.add(Marker(map).apply {
            position = GeoPoint(location.latitude, location.longitude)
            icon = dot
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "Você está aqui"
            setOnMarkerClickListener { _, _ -> true }
        })
    }

    points.sortedBy { it.id == selectedId }.forEach { point ->
        val selected = point.id == selectedId
        map.overlays.add(Marker(map).apply {
            position = GeoPoint(point.latitude, point.longitude)
            icon = holder.icons.getOrPut(point.type to selected) { pinDrawable(map.context, point.type, selected) }
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = point.name
            setOnMarkerClickListener { _, _ ->
                onPointClick(point.id)
                true
            }
        })
    }
    map.invalidate()
}

/** Os PNGs da legenda redimensionados para um tamanho fixo em dp. */
private fun pinDrawable(context: Context, type: CollectionPointType, selected: Boolean): Drawable {
    val sizePx = dpToPx(context, if (selected) PIN_SELECTED_SIZE_DP else PIN_SIZE_DP)
    val source = ContextCompat.getDrawable(context, type.iconRes)!!
    val ratio = source.intrinsicHeight.toFloat() / source.intrinsicWidth.coerceAtLeast(1)
    val bitmap = source.toBitmap(sizePx, (sizePx * ratio).toInt().coerceAtLeast(1))
    return pixelExact(context, bitmap)
}

/** O bitmap já está em pixels da tela. */
private fun pixelExact(context: Context, bitmap: android.graphics.Bitmap): Drawable {
    bitmap.density = context.resources.displayMetrics.densityDpi
    return BitmapDrawable(context.resources, bitmap)
}

/** Bolinha azul de "você está aqui". */
private fun userDotDrawable(context: Context): Drawable {
    val size = dpToPx(context, USER_DOT_SIZE_DP)
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(0xFF1E88E5.toInt())
        setStroke(dpToPx(context, 3), 0xFFFFFFFF.toInt())
        setSize(size, size)
    }.let { pixelExact(context, it.toBitmap(size, size)) }
}

private fun dpToPx(context: Context, dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()
