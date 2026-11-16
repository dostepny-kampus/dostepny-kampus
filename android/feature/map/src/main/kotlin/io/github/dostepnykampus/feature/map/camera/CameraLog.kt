/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import io.github.dostepnykampus.feature.map.campus.CampusBounds
import java.util.Locale
import kotlin.math.roundToInt

/** Widok w logu: środek (szerokość, długość) i powiększenie, np. „51.10900,17.06000 z18.20”. */
internal fun MapCamera.logText(): String =
    "%.5f,%.5f z%.2f".format(Locale.ROOT, center.latitude, center.longitude, zoom)

/** Prostokąt w logu: zachód, południe, wschód, północ. */
internal fun CampusBounds.logText(): String = "%.5f,%.5f..%.5f,%.5f".format(Locale.ROOT, west, south, east, north)

/**
 * Linia wyboru celu kamery dla budynku [building]: teren [area] (`null` to teren startowy), granica [limits]
 * przycinająca cel, wolna mapa [free] i ruch [move].
 */
internal fun pickLine(
    building: String?,
    area: String?,
    limits: MapLimits?,
    free: FreeMap,
    move: CameraTransition,
): String {
    val size = "${free.widthDp.roundToInt()}×${free.heightDp.roundToInt()} dp"
    val border = limits?.run { bounds.logText() } ?: "brak"
    val place = "teren ${area ?: "startowy"}, granica $border"
    return "Kamera: cel ${building ?: "brak"}, $place, wolna mapa $size, ${moveLine(move)}"
}

/** Linia startu ruchu [move] z dopełnieniem [inset] w pikselach. */
internal fun startLine(move: CameraTransition, inset: MapInset): String =
    "Kamera: start ${moveLine(move)}, dopełnienie ${inset.logText()}"

/** Linia bez ruchu: kamera już stoi w widoku celu. */
internal fun stayLine(at: MapCamera): String = "Kamera: bez ruchu, już w ${at.logText()}"

/** Linia końca ruchu ([isDone]) albo przerwania; [isCurrent] fałsz dla ruchu zastąpionego nowym. */
internal fun endLine(isDone: Boolean, isCurrent: Boolean, at: MapCamera): String {
    val end = if (isDone) "koniec" else "przerwany"
    val old = if (isCurrent) "" else " (zastąpiony)"
    return "Kamera: $end$old w ${at.logText()}"
}

/** Linia dosunięcia samego dopełnienia [inset] po ruchu. */
internal fun paddingLine(inset: MapInset): String = "Kamera: dopełnienie ${inset.logText()}"

private fun moveLine(move: CameraTransition): String =
    "${move.motion} ${move.durationMillis} ms do ${move.camera.logText()}"

private fun MapInset.logText(): String = "góra ${top.roundToInt()} px, dół ${bottom.roundToInt()} px"
