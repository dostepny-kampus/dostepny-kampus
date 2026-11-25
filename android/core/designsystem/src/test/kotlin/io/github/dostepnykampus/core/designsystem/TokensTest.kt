/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import java.io.File
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

private const val MATERIAL_TEXT_SLOT_COUNT = 15

/**
 * Kolory, typografia i wymiary motywu pochodzą z pliku tokenów.
 *
 * Wymagania: NFR-16, NFR-20, NFR-17
 */
class TokensTest {
    private val tokens =
        File(requireNotNull(System.getProperty("tokens.file")) { "Brak ścieżki pliku tokenów" })
            .inputStream()
            .use { Load(LoadSettings.builder().build()).loadFromInputStream(it) }
            .asMap()

    private val themes =
        listOf(
            Triple("jasny", LightColorScheme, LightStatusColors) to LightMapColors,
            Triple("ciemny", DarkColorScheme, DarkStatusColors) to DarkMapColors,
        )

    @Test
    fun takesColorsFromTokens() {
        themes.forEach { (theme, mapColors) ->
            val (name, scheme, statusColors) = theme
            tokens.sub("kolory").forEach { (key, value) ->
                val holder: Any = if (key.contains("status", ignoreCase = true)) statusColors else scheme
                assertEquals("$name $key", color(value.asMap().getValue(name)), holder.property(key))
            }
            tokens.sub("mapa").sub("kolory").forEach { (key, value) ->
                assertEquals("$name mapa $key", color(value.asMap().getValue(name)), mapColors.property(camel(key)))
            }
        }
    }

    @Test
    fun takesTypographyFromTokens() {
        val typography = typography(FontFamily.Default)
        val section = tokens.sub("typografia")
        section.sub("style").forEach { (key, value) ->
            val expected = value.asMap()
            val style = checkNotNull(typography.property(key)) as TextStyle
            assertEquals("$key sp", expected.getValue("sp").int().sp, style.fontSize)
            assertEquals("$key interlinia", expected.getValue("interlinia_sp").int().sp, style.lineHeight)
            assertEquals("$key waga", FontWeight(expected.getValue("waga").int()), style.fontWeight)
            assertTrue("$key minimum_sp", style.fontSize.value >= section.getValue("minimum_sp").int())
        }
        assertEquals(FontWeight(section.getValue("wyroznienie_waga").int()), Typografia.wyroznienieWaga)
    }

    /**
     * Każdy publiczny slot `Typography` ma krój motywu i co najmniej `minimum_sp`, a slot spoza skali ma styl
     * swojego zastępstwa. Komponenty Material same sięgają po sloty spoza skali, więc żaden nie może zostać
     * domyślny. Wymagania: NFR-16, NFR-20
     */
    @Test
    fun slotsHaveAppFontAndMinimum() {
        val font = FontFamily.Serif
        val typography = typography(font)
        val section = tokens.sub("typografia")
        val minimum = section.getValue("minimum_sp").int()
        val slots =
            Typography::class.memberProperties.filter {
                it.visibility == KVisibility.PUBLIC && it.returnType.classifier == TextStyle::class
            }
        assertEquals("sloty Material 3", MATERIAL_TEXT_SLOT_COUNT, slots.size)
        slots.forEach { slot ->
            val style = checkNotNull(slot.getter.call(typography)) as TextStyle
            assertEquals("${slot.name} krój", font, style.fontFamily)
            assertTrue("${slot.name} ${style.fontSize} < $minimum sp", style.fontSize.value >= minimum)
        }
        section.sub("zastepstwa").forEach { (slot, target) ->
            assertEquals("zastępstwo $slot", typography.property(target.toString()), typography.property(slot))
        }
    }

    /**
     * Rozmiar napisów na mapie z `mapa.napisy_sp` i podpisu OpenStreetMap z `mapa.podpis_osm_sp`.
     * Wymagania: NFR-16, NFR-37
     */
    @Test
    fun takesMapLabelSizeFromTokens() {
        val map = tokens.sub("mapa")
        assertEquals("napisy mapy", map.getValue("napisy_sp").int().sp, Typografia.napisyMapy)
        assertEquals("podpis OpenStreetMap", map.getValue("podpis_osm_sp").int().sp, Typografia.podpisOsm)
    }

    /** Grubości, progi powiększenia i krycia mapy z podsekcji `mapa` poza kolorami. Wymagania: NFR-16, NFR-21 */
    @Test
    fun takesMapSectionsFromTokens() {
        val generated =
            mapOf(
                "plan" to MapaPlan,
                "sciezka" to MapaSciezka,
                "przygaszenie" to MapaPrzygaszenie,
                "kontur" to MapaKontur,
            )
        val sections = tokens.sub("mapa").filter { (name, value) -> name != "kolory" && value is Map<*, *> }
        assertEquals(sections.keys, generated.keys)
        generated.forEach { (section, holder) ->
            sections.getValue(section).asMap().forEach { (key, value) ->
                generatedValueOrNull(value)?.let { assertEquals("mapa $section $key", it, holder.property(camel(key))) }
            }
        }
    }

    @Test
    fun takesSizesFromTokens() {
        val generated =
            mapOf(
                "odstepy" to Odstepy,
                "cele_dotyku" to CeleDotyku,
                "obramowania" to Obramowania,
                "zaokraglenia" to Zaokraglenia,
                "ikony" to Ikony,
                "wysokosci" to Wysokosci,
            )
        val sections = tokens.filterValues { v -> v is Map<*, *> && v.values.all { it is Map<*, *> && "dp" in it } }
        assertEquals(sections.keys, generated.keys)
        generated.forEach { (section, holder) ->
            tokens.sub(section).forEach { (key, value) ->
                val entry = value.asMap()
                assertEquals("$section $key", entry.getValue("dp").int().dp, holder.property(camel(key)))
                entry.filterValues { it is String && it.startsWith("#") }.forEach { (field, hex) ->
                    assertEquals("$section $key $field", color(hex), holder.property(camel("${key}_$field")))
                }
            }
        }
        val corners = tokens.sub("zaokraglenia")
        mapOf("small" to "standard", "medium" to "srednie", "large" to "duze", "extraLarge" to "bardzo_duze")
            .forEach { (slot, token) ->
                val radius = corners.sub(token).getValue("dp").int()
                assertEquals(slot, RoundedCornerShape(radius.dp), AppShapes.property(slot))
            }
    }

    private fun Any.property(name: String): Any? =
        this::class
            .memberProperties
            .single { it.name == name }
            .getter
            .call(this)

    private fun color(hex: Any) = Color(("FF" + hex.toString().removePrefix("#")).toLong(radix = 16))

    private fun camel(name: String) =
        name
            .split('_')
            .mapIndexed { index, part -> if (index == 0) part else part.replaceFirstChar(Char::uppercase) }
            .joinToString("")

    private fun Any.int() = (this as Number).toInt()

    private fun Any.double() = (this as Number).toDouble()

    private fun generatedValueOrNull(token: Any): Any? {
        val stops =
            (token as? List<*>)?.run {
                filterIsInstance<Map<*, *>>().filter { "powiekszenie" in it }.takeIf { it.size == size }
            }
        return if (token is Number) token.double() else stops?.map(::zoomStop)
    }

    private fun zoomStop(stop: Map<*, *>): ZoomStop {
        val entry = stop.asMap()
        return ZoomStop(zoom = entry.getValue("powiekszenie").double(), dp = entry.getValue("dp").double())
    }

    private fun Any.asMap(): Map<String, Any> =
        (this as Map<*, *>).entries.associate { checkNotNull(it.key).toString() to checkNotNull(it.value) }

    private fun Map<String, Any>.sub(key: String) = getValue(key).asMap()
}
