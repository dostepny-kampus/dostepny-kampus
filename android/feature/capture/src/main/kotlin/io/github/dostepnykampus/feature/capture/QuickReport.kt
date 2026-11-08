/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind

/**
 * Zgłoszenie szybkie V4 po kroku 2: zdjęcie, grupa barier i odpowiedź na pytanie o piętro (US4.1, US4.3, US4.5).
 *
 * @property photo zdjęcie z kroku 1
 * @property group grupa barier z kroku 2
 * @property floorNumber piętro z kroku 2; `null` to „Nie wiem” albo brak pytania (BR-14)
 */
data class QuickReport(val photo: PendingPhotoDraft, val group: BarrierGroup, val floorNumber: Int? = null)

/**
 * Bieżąca pozycja do zgłoszenia szybkiego (US4.1, NFR-27).
 *
 * @property point miejsce, w którym stoi Odwiedzający
 * @property accuracyMeters dokładność pozycji w metrach
 * @property isInBuilding czy pozycja leży w konturze budynku (BR-G2)
 */
data class QuickPosition(val point: GeoPoint, val accuracyMeters: Double, val isInBuilding: Boolean)

/**
 * Czy krok 2 pyta o piętro po wyborze grupy (BR-14): w konturze budynku i przy każdej grupie poza „Coś innego”.
 * Gdy pozycja jeszcze nie jest znana ([isInBuilding] `null`), pyta jak w konturze.
 */
fun BarrierGroup.asksFloor(isInBuilding: Boolean?): Boolean = this != BarrierGroup.OTHER && isInBuilding != false

/**
 * Treść zgłoszenia szybkiego z bieżącą pozycją; budynek ustala serwer z konturu (BR-14). Poza konturem
 * zgłoszenie idzie bez piętra, także gdy Odwiedzający je wybrał, zanim telefon ustalił pozycję.
 */
fun QuickReport.content(position: QuickPosition): SubmissionContent =
    SubmissionContent(
        kind = SubmissionKind.QUICK,
        location = position.point,
        photoIds = listOf(photo.photoId),
        accuracyMeters = position.accuracyMeters,
        floor = floorNumber?.takeIf { position.isInBuilding }?.let { SubmissionFloor(number = it) },
        barrierGroup = group,
    )
