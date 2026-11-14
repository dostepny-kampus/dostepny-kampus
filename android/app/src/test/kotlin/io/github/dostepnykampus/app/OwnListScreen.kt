/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.dostepnykampus.core.data.di.DataModule
import io.github.dostepnykampus.core.data.submission.own.OwnStatuses
import io.github.dostepnykampus.core.data.submission.own.OwnSubmissions
import io.github.dostepnykampus.core.data.submission.own.StatusRefresh
import io.github.dostepnykampus.core.data.submissions.SubmissionStore
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsActions
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsContent
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

/** Model listy „Moje zgłoszenia” z historią odczytaną teraz z [store], bez sieci. */
internal fun offlineOwnList(store: SubmissionStore): MySubmissionsViewModel {
    val list = runBlocking { DataModule.ownSubmissions(store).observe().first() }
    return MySubmissionsViewModel(
        history = OwnSubmissions { flowOf(list) },
        statuses = OwnStatuses { StatusRefresh.OFFLINE },
        networkStatus = { flowOf(false) },
    )
}

/** Lista „Moje zgłoszenia” z modelu [viewModel], bez działań nawigacji. */
@Composable
internal fun OwnListScreen(viewModel: MySubmissionsViewModel) {
    val state by viewModel.state.collectAsState()
    DostepnyKampusTheme {
        MySubmissionsContent(
            state = state,
            actions = MySubmissionsActions(onBack = {}, onMeasure = {}, onOpen = {}),
        )
    }
}
