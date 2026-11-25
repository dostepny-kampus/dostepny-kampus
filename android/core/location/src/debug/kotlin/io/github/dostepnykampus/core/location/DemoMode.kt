/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import android.content.Context

private const val PREFERENCES = "tryb_pokazu"
private const val ENABLED = "wlaczony"

/**
 * Włącznik trybu pokazu zapisany w telefonie. Domyślnie wyłączony; zmiana działa od następnego otwarcia ekranu C1.
 * TODO(#3817): przełącznik w aplikacji debug.
 *
 * Wymagania: US6.1
 */
@TrybPokazu
public object DemoMode {
    /** Czy pomiar C1 bierze położenia i satelity z trasy pokazu zamiast z odbiornika GNSS. */
    public fun isEnabled(context: Context): Boolean = context.preferences().getBoolean(ENABLED, false)

    /** Zapisuje stan włącznika [isEnabled]. */
    public fun setEnabled(context: Context, isEnabled: Boolean) {
        context.preferences().edit().putBoolean(ENABLED, isEnabled).apply()
    }

    private fun Context.preferences() = applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
