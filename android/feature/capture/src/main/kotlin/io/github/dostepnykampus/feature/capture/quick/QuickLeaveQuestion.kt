/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.feature.capture.R

/**
 * V4, potwierdzenie rezygnacji przed usunięciem zrobionego zdjęcia: komunikat i dwa przyciski tej samej wielkości,
 * bez limitu czasu.
 *
 * Wymagania: US4.8, UC7, NFR-23
 *
 * @param onLeave „Tak, zrezygnuj”: zdjęcie znika, a zgłoszenie nie powstaje
 * @param onStay „Wróć do zdjęcia”: zdjęcie i grupa zostają
 */
@Composable
internal fun QuickLeaveQuestion(onLeave: () -> Unit, onStay: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(stringResource(R.string.szybkie_rezygnacja))
        LeaveChoice(text = stringResource(R.string.szybkie_tak_zrezygnuj), onClick = onLeave)
        LeaveChoice(text = stringResource(R.string.szybkie_wroc_do_zdjecia), onClick = onStay)
    }
}

@Composable
private fun LeaveChoice(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk)) {
        Text(text)
    }
}
