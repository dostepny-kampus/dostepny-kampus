/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.BackHeader
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import kotlinx.coroutines.flow.filter
import java.io.File

/**
 * V4 Zgłoszenie szybkie na kroku z [route]: zdjęcie, grupa barier z piętrem, wysłanie i potwierdzenie (US4.1).
 * Kroki przed wysłaniem wymagają zgody na dokładną lokalizację (NFR-27, BR-8).
 * Krok 1: „Zrób zdjęcie” otwiera aparat, a zdjęcie od razu prowadzi do kroku 2; po powrocie z kroku 2 jest „Dalej”.
 * Krok 2: grupa, a w konturze albo przy nieznanej pozycji pytanie o piętro; „Wstecz” z pytania wraca do grup.
 * „Tak, zrezygnuj”: zdjęcie znika z telefonu, a zgłoszenie nie powstaje (UC7 1b).
 *
 * @param route krok z danymi wcześniejszych kroków
 * @param actions czynności nawigatora
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun QuickReportScreen(route: ReportRoute, actions: QuickReportActions, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            // „Wstecz” w kroku 1 ze zdjęciem najpierw pyta o rezygnację; wstecz w pytaniu to „Wróć do zdjęcia”.
            val isPhotoTaken = route.step == ReportStep.PHOTO && route.photoPath != null
            var isLeaving by rememberSaveable { mutableStateOf(false) }
            BackHandler(enabled = isPhotoTaken) { isLeaving = !isLeaving }
            val back: () -> Unit = {
                if (isPhotoTaken) {
                    isLeaving = true
                } else {
                    actions.onBack()
                }
            }
            BackHeader(title = stringResource(R.string.szybkie_naglowek), onBack = back)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
                verticalArrangement = Arrangement.spacedBy(Odstepy.m),
            ) {
                when {
                    route.step == ReportStep.SENT -> {
                        QuickSentMessage()
                        MainButton(text = stringResource(R.string.szybkie_wroc_do_mapy), onClick = actions.onBackToMap)
                    }

                    isLeaving -> {
                        QuickLeaveQuestion(onLeave = { leave(route, actions) }, onStay = { isLeaving = false })
                    }

                    else -> {
                        Text(stringResource(R.string.szybkie_krok, route.step.ordinal + 1))
                        WithLocationPermission(onBack = actions.onBack) { QuickStep(route, actions) }
                    }
                }
            }
        }
    }
}

private fun leave(route: ReportRoute, actions: QuickReportActions) {
    route.photoPath?.let { File(it).delete() }
    actions.onBack()
}

@Composable
private fun QuickStep(route: ReportRoute, actions: QuickReportActions) {
    when (route.step) {
        ReportStep.PHOTO -> {
            QuickPhotoStep(route = route, onNext = actions.onNext)
        }

        ReportStep.GROUP -> {
            QuickGroupStep(route = route, onNext = actions.onNext)
        }

        // Potwierdzenie SENT pokazuje QuickReportScreen bez kroków, więc tu trafia tylko krok 3.
        ReportStep.SEND, ReportStep.SENT -> {
            route.quickReport()?.let { QuickSendStep(route = route, report = it, onSend = actions.onSent) }
        }
    }
}

@Composable
private fun QuickPhotoStep(route: ReportRoute, onNext: (ReportRoute) -> Unit) {
    val takePhoto = rememberPhotoCamera(onPhoto = { onNext(route.copy(photoId = it.photoId, photoPath = it.path)) })
    PhotoSection(photos = PhotoPaths(listOfNotNull(route.photoPath)), onTakePhoto = takePhoto)
    if (route.photoPath != null) MainButton(text = stringResource(R.string.pomiar_dalej), onClick = { onNext(route) })
}

@Composable
private fun QuickGroupStep(route: ReportRoute, onNext: (ReportRoute) -> Unit) {
    var asked by rememberSaveable { mutableStateOf<BarrierGroup?>(null) }
    val group = asked
    if (group == null) {
        QuickGroupQuestion(onGroup = { chosen ->
            if (chosen.asksFloor(isInBuilding = null)) asked = chosen else onNext(route.copy(group = chosen))
        })
    } else {
        BackHandler { asked = null }
        QuickFloorQuestion(group = group, onFloor = { floor ->
            onNext(route.copy(group = group, floorNumber = floor, isFloorAsked = true))
        })
    }
}

@Composable
private fun QuickSendStep(
    route: ReportRoute,
    report: QuickReport,
    onSend: () -> Unit,
    viewModel: QuickSendViewModel = hiltViewModel(),
) {
    val position by viewModel.position.collectAsStateWithLifecycle()
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val sent by rememberUpdatedState(onSend)
    LaunchedEffect(viewModel) { viewModel.stage.filter { it == QuickSendStage.SENT }.collect { sent() } }
    QuickSendSummary(report = report, isFloorAsked = route.isFloorAsked, isPositionKnown = position != null)
    if (stage == QuickSendStage.FAILED) Text(stringResource(R.string.formularz_blad_zapisu))
    MainButton(
        text = stringResource(R.string.szybkie_wyslij),
        onClick = { viewModel.send(report) },
        isEnabled = position != null && stage != QuickSendStage.SENDING,
    )
}

@Composable
private fun MainButton(text: String, onClick: () -> Unit, isEnabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk),
    ) {
        Text(text)
    }
}
