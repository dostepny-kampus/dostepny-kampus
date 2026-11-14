/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import io.github.dostepnykampus.feature.capture.R
import io.github.dostepnykampus.feature.capture.location.WithLocationPermission
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import io.github.dostepnykampus.feature.capture.photo.PhotoPaths
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.photo.rememberPhotoCamera
import io.github.dostepnykampus.feature.capture.report.PhotoSection
import kotlinx.coroutines.flow.filter
import java.io.File

/**
 * V4 Zgłoszenie szybkie na kroku z [route]: zdjęcie, grupa barier z piętrem, wysłanie i potwierdzenie; kroki przed
 * wysłaniem wymagają zgody na dokładną lokalizację, a pozycja jest ustalana od kroku 1. Krok 1: „Zrób zdjęcie” otwiera
 * aparat i prowadzi do kroku 2; „Wstecz” ze zdjęciem pyta o rezygnację, a „Tak, zrezygnuj” usuwa zdjęcie. Krok 2:
 * grupa, a w konturze albo przy nieznanej pozycji pytanie o piętro. Krok 3: „Brak pozycji” ma „Spróbuj ponownie” i
 * „Zrezygnuj”, a „Tak, zrezygnuj” otwiera mapę. Po wysłaniu ekran pokazuje tylko potwierdzenie (`SENT`).
 *
 * Wymagania: US4.1, NFR-27, BR-8, BR-14, BR-G2, UC7
 *
 * @param route krok z danymi wcześniejszych kroków
 * @param actions czynności nawigatora
 * @param modifier wymiary nadane przez aplikację
 * @param models modele widoku podane z zewnątrz; brakujące z grafu Hilt
 */
@Composable
fun QuickReportScreen(
    route: ReportRoute,
    actions: QuickReportActions,
    modifier: Modifier = Modifier,
    models: QuickReportModels = QuickReportModels(),
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            val isPhotoTaken = route.step == ReportStep.PHOTO && route.photoPath != null
            var isLeaving by rememberSaveable { mutableStateOf(false) }
            BackHandler(enabled = isPhotoTaken || isLeaving) { isLeaving = !isLeaving }
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
                        QuickSentMessage(isSavedOffline = route.isSavedOffline)
                        MainButton(text = stringResource(R.string.szybkie_wroc_do_mapy), onClick = actions.onBackToMap)
                    }

                    isLeaving -> {
                        QuickLeaveQuestion(onLeave = { leave(route, actions) }, onStay = { isLeaving = false })
                    }

                    else -> {
                        QuickStepProgress(step = route.step.ordinal + 1)
                        WithLocationPermission(onBack = actions.onBack) {
                            QuickStep(
                                route = route,
                                actions = actions,
                                models = models,
                                onGiveUp = { isLeaving = true },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun leave(route: ReportRoute, actions: QuickReportActions) {
    route.photoPath?.let { File(it).delete() }
    if (route.step == ReportStep.PHOTO) actions.onBack() else actions.onBackToMap()
}

@Composable
private fun QuickStep(
    route: ReportRoute,
    actions: QuickReportActions,
    models: QuickReportModels,
    onGiveUp: () -> Unit,
) {
    val where = models.sendModel()
    when (route.step) {
        ReportStep.PHOTO -> {
            QuickPositionWarmUp(where)
            val entrances = models.entrances
            val photoTranscoder = models.photoTranscoder
            route.entranceId?.let { id ->
                if (entrances == null) QuickEntranceLine(id = id) else QuickEntranceLine(id = id, entrances = entrances)
            }
            if (photoTranscoder == null) {
                QuickPhotoStep(route = route, onNext = actions.onNext)
            } else {
                QuickPhotoStep(route = route, onNext = actions.onNext, transcoder = photoTranscoder)
            }
        }

        ReportStep.GROUP -> {
            QuickGroupStep(route = route, onNext = actions.onNext, where = where)
        }

        ReportStep.SEND, ReportStep.SENT -> {
            route.quickReport()?.let { report ->
                QuickSendStep(
                    route = route,
                    report = report,
                    onSend = actions.onSent,
                    onGiveUp = onGiveUp,
                    viewModel = where,
                )
            }
        }
    }
}

@Composable
private fun QuickReportModels.sendModel(): QuickSendViewModel = send ?: hiltViewModel()

@Composable
private fun QuickPositionWarmUp(where: QuickSendViewModel) {
    where.lookup.collectAsStateWithLifecycle()
}

@Composable
private fun QuickPhotoStep(
    route: ReportRoute,
    onNext: (ReportRoute) -> Unit,
    transcoder: PhotoTranscoder = hiltViewModel(),
) {
    val camera =
        rememberPhotoCamera(
            onPhoto = { onNext(route.copy(photoId = it.photoId, photoPath = it.path)) },
            transcoder = transcoder,
        )
    PhotoSection(photos = PhotoPaths(listOfNotNull(route.photoPath)), camera = camera)
    if (route.photoPath != null) MainButton(text = stringResource(R.string.pomiar_dalej), onClick = { onNext(route) })
}

@Composable
private fun QuickEntranceLine(id: String, entrances: QuickEntranceViewModel = hiltViewModel()) {
    val updates = remember(entrances, id) { entrances.entrance(id) }
    val entrance by updates.collectAsStateWithLifecycle(initialValue = null)
    entrance?.let { chosen ->
        val name = chosen.name
        Text(
            when {
                name != null -> stringResource(R.string.szybkie_wejscie, name)
                chosen.isMain -> stringResource(R.string.szybkie_wejscie_glowne)
                else -> stringResource(R.string.szybkie_wejscie_bez_nazwy)
            },
        )
    }
}

@Composable
private fun QuickGroupStep(route: ReportRoute, onNext: (ReportRoute) -> Unit, where: QuickSendViewModel) {
    var asked by rememberSaveable { mutableStateOf<BarrierGroup?>(null) }
    val lookup by where.lookup.collectAsStateWithLifecycle()
    val isInBuilding = (lookup as? QuickLookup.Found)?.run { position.isInBuilding }
    val floors by where.floorChoices.collectAsStateWithLifecycle()
    val group = asked
    if (group == null) {
        QuickGroupQuestion(selected = route.group, onGroup = { chosen ->
            if (chosen.asksFloor(isInBuilding)) asked = chosen else onNext(route.copy(group = chosen))
        })
    } else {
        BackHandler { asked = null }
        QuickFloorQuestion(group = group, floors = QuickFloors(floors), onFloor = { answer ->
            val floor = answer.floor
            val answered = route.copy(floorNumber = answer.number, floorId = floor?.id, floorLabel = floor?.label)
            onNext(answered.copy(group = group, isFloorAsked = true))
        })
    }
}

@Composable
private fun QuickSendStep(
    route: ReportRoute,
    report: QuickReport,
    onSend: (isSavedOffline: Boolean) -> Unit,
    onGiveUp: () -> Unit,
    viewModel: QuickSendViewModel = hiltViewModel(),
) {
    val lookup by viewModel.lookup.collectAsStateWithLifecycle()
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val sent by rememberUpdatedState(onSend)
    LaunchedEffect(viewModel) {
        viewModel.stage
            .filter { it == QuickSendStage.SENT || it == QuickSendStage.SAVED_OFFLINE }
            .collect { sent(it == QuickSendStage.SAVED_OFFLINE) }
    }
    QuickSendSummary(report = report, isFloorAsked = route.isFloorAsked, lookup = lookup)
    if (lookup == QuickLookup.Lost) {
        MainButton(text = stringResource(R.string.formularz_sprobuj_ponownie), onClick = viewModel::retry)
        OutlinedButton(onClick = onGiveUp, modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk)) {
            Text(stringResource(R.string.szybkie_zrezygnuj))
        }
    } else {
        if (stage == QuickSendStage.FAILED) Text(stringResource(R.string.formularz_blad_zapisu))
        MainButton(
            text = stringResource(R.string.szybkie_wyslij),
            onClick = { viewModel.send(report) },
            isEnabled = lookup is QuickLookup.Found && stage != QuickSendStage.SENDING,
        )
    }
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
