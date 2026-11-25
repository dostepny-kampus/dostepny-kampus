/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.GroupFrame
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.marking.BarrierPhoto
import io.github.dostepnykampus.core.designsystem.marking.SeverityIcon
import io.github.dostepnykampus.core.designsystem.marking.barrierPlace
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.feature.review.queue.kindRes
import io.github.dostepnykampus.feature.review.queue.titleRes

/**
 * Dane zgłoszenia na R2 bez pól do edycji: rodzaj, typ albo grupa, uciążliwość ikoną i napisem, miejsce i zdjęcia.
 *
 * Wymagania: US10, BR-14, BR-15, NFR-21
 *
 * @param submission zgłoszenie
 * @param photoUrls adresy zdjęć zgłoszenia
 */
@Composable
internal fun SubmissionDetails(submission: Submission, photoUrls: List<String>) {
    val content = submission.content
    val type = content.barrierType?.let { BarrierType.fromCode(it) }
    GroupFrame {
        Text(stringResource(content.kindRes()), style = MaterialTheme.typography.titleSmall)
        Text(
            text = stringResource(content.titleRes()),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
        )
        content.severity?.let { Severity.fromValue(it) }?.let { severity ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SeverityIcon(severity)
                Text(stringResource(severity.nameRes()), style = MaterialTheme.typography.labelLarge)
            }
        }
        Text(barrierPlace(symbol = content.buildingSymbol, name = null, entrance = null, floor = content.floor))
        photoUrls.forEach { url -> BarrierPhoto(url = url, type = type, modifier = Modifier.fillMaxWidth()) }
    }
}
