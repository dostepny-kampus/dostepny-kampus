/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.data.CampusMapData
import okhttp3.Call
import okhttp3.OkHttpClient
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import java.util.Locale
import kotlin.math.ceil

private const val LABEL_HALO_PER_TEXT = 0.25f
private const val RGB_MASK = 0xFFFFFF

/** Widok mapy za interfejsem: ekran i jego testy nie zależą od natywnej biblioteki mapy. */
fun interface CampusMapView {
    /**
     * Mapa [data] w widoku [camera]. Po geście widok zgłasza nowy widok przez [onCameraIdle], a po dotknięciu
     * konturu albo miejsca obok niego symbole trafionych budynków przez [onBuildingsTouch].
     */
    @Composable
    fun Show(
        data: CampusMapData,
        camera: MapCamera,
        onCameraIdle: (MapCamera) -> Unit,
        onBuildingsTouch: (List<String>) -> Unit,
        modifier: Modifier,
    )
}

/**
 * Klient HTTP mapy z własnym nagłówkiem User-Agent aplikacji, jak wymaga polityka kafelków OSM (NFR-39). Bez
 * regionów offline: kafelki tylko na bieżący widok.
 */
internal fun osmClient(packageName: String): Call.Factory {
    val client = OkHttpClient()
    val agent = "DostepnyKampus ($packageName)"
    return Call.Factory { request ->
        client.newCall(request.newBuilder().header(name = "User-Agent", value = agent).build())
    }
}

/** Obraz podpisu konturu: tekst `napis_pwr` z obwódką `obwodka` (para kontrastu z tokenów, NFR-16). */
internal fun labelImage(label: String, colors: MapColors, size: Float): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.textSize = size
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.strokeWidth = size * LABEL_HALO_PER_TEXT
    paint.strokeJoin = Paint.Join.ROUND
    val margin = paint.strokeWidth
    val width = ceil(paint.measureText(label) + 2 * margin).toInt()
    val height = ceil(paint.descent() - paint.ascent() + 2 * margin).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    listOf(Paint.Style.STROKE to colors.obwodka, Paint.Style.FILL to colors.napisPwr).forEach { (style, color) ->
        paint.style = style
        paint.color = color.toArgb()
        canvas.drawText(label, margin, margin - paint.ascent(), paint)
    }
    return bitmap
}

internal fun MapColors.styleColors(): MapStyleColors =
    MapStyleColors(land = lad.hex(), building = pwr.hex(), buildingOutline = pwrKontur.hex())

internal fun Color.hex(): String = "#%06X".format(Locale.ROOT, toArgb() and RGB_MASK)

internal fun MapCamera.toPosition(): CameraPosition =
    CameraPosition
        .Builder()
        .target(LatLng(latitude = center.latitude, longitude = center.longitude))
        .zoom(zoom)
        .build()

internal fun CameraPosition.toCamera(): MapCamera =
    MapCamera(GeoPoint(longitude = target?.longitude ?: 0.0, latitude = target?.latitude ?: 0.0), zoom)
