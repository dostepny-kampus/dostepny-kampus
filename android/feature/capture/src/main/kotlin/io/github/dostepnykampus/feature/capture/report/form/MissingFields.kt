/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.form

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import io.github.dostepnykampus.core.designsystem.Odstepy

/**
 * Pola C2, do których prowadzi „Zapisz” z brakami: przewinięcie do pola i fokus na komunikacie przy nim.
 *
 * Wymagania: NFR-15, NFR-18
 */
@Stable
internal class MissingFields {
    private val views = ReportFormError.entries.associateWith { BringIntoViewRequester() }
    private val focuses = ReportFormError.entries.associateWith { FocusRequester() }

    internal fun view(error: ReportFormError): BringIntoViewRequester = views.getValue(error)

    internal fun focus(error: ReportFormError): FocusRequester = focuses.getValue(error)

    internal suspend fun reveal(error: ReportFormError) {
        views.getValue(error).bringIntoView()
        focuses.getValue(error).requestFocus()
    }
}

/**
 * Pola C2 z brakami; po każdym „Zapisz” z brakami, czyli zmianie [attempts], prowadzi do pierwszego braku.
 *
 * Wymagania: NFR-15, NFR-18
 *
 * @param errors braki w kolejności pól na ekranie
 * @param attempts liczba prób zapisu z brakami
 */
@Composable
internal fun rememberMissingFields(errors: List<ReportFormError>, attempts: Int): MissingFields {
    val fields = remember { MissingFields() }
    val shown by rememberUpdatedState(errors)
    LaunchedEffect(attempts) {
        if (attempts > 0) shown.firstOrNull()?.let { fields.reveal(it) }
    }
    return fields
}

/**
 * Pole formularza z komunikatem braku [error] pod nim; cel przewinięcia i fokusu z [fields].
 *
 * Wymagania: NFR-15, NFR-18
 *
 * @param error brak, który dotyczy tego pola
 * @param errors braki pokazane na ekranie
 * @param fields cele przewinięcia i fokusu
 * @param content pole formularza
 */
@Composable
internal fun MissingField(
    error: ReportFormError,
    errors: List<ReportFormError>,
    fields: MissingFields,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.bringIntoViewRequester(fields.view(error)),
        verticalArrangement = Arrangement.spacedBy(Odstepy.m),
    ) {
        content()
        FieldError(
            error = error,
            isShown = error in errors,
            modifier = Modifier.focusRequester(fields.focus(error)).focusable(),
        )
    }
}
