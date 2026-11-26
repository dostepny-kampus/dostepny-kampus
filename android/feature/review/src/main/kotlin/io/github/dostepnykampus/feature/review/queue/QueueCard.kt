/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.campusDay
import io.github.dostepnykampus.core.designsystem.card.CardAction
import io.github.dostepnykampus.core.designsystem.card.ListItemCard
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.review.R
import io.github.dostepnykampus.core.designsystem.R as DesignR

/**
 * Pozycja R1: typ albo grupa barier, budynek, rodzaj zgłoszenia, data wysłania i czynność.
 *
 * Wymagania: US10.4, US10.7, BR-14
 *
 * @param submission zgłoszenie
 * @param isMine czy zgłoszenie wziął ten Weryfikator: „Otwórz” zamiast „Weź do sprawdzenia”
 * @param onAction wykonanie czynności
 */
@Composable
internal fun QueueCard(submission: Submission, isMine: Boolean, onAction: () -> Unit) {
    val content = submission.content
    ListItemCard {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
        ) {
            Text(text = stringResource(content.titleRes()), style = MaterialTheme.typography.titleMedium)
            Text(text = buildingText(content))
            Text(text = stringResource(content.kindRes()))
            Text(text = stringResource(R.string.kolejka_wyslane, campusDay(submission.createdAt)))
        }
        val action = if (isMine) R.string.kolejka_otworz else R.string.kolejka_wez
        CardAction(text = stringResource(action), onClick = onAction, opensScreen = isMine)
    }
}

internal fun SubmissionContent.titleRes(): Int =
    barrierType?.let { BarrierType.fromCode(it) }?.nameRes()
        ?: barrierGroup?.nameRes()
        ?: entranceMarking?.let { if (it == EntranceMarking.ACCESSIBLE) ACCESSIBLE else INACCESSIBLE }
        ?: kindRes()

internal fun SubmissionContent.kindRes(): Int =
    when {
        isBarrierRemoved == true -> R.string.kolejka_bariery_nie_ma
        kind == SubmissionKind.PRECISE -> R.string.kolejka_pomiar
        kind == SubmissionKind.QUICK -> R.string.kolejka_szybkie
        else -> R.string.kolejka_zmiana_oznaczenia
    }

@Composable
@ReadOnlyComposable
private fun buildingText(content: SubmissionContent): String =
    content.buildingSymbol?.let { stringResource(DesignR.string.miejsce_budynek, it) }
        ?: stringResource(DesignR.string.miejsce_poza)

private val ACCESSIBLE = DesignR.string.oznaczenie_dostepne
private val INACCESSIBLE = DesignR.string.oznaczenie_niedostepne
