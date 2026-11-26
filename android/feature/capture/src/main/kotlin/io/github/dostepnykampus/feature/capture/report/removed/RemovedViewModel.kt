/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.removed

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.NearestEntrance
import io.github.dostepnykampus.core.data.submission.pending.PendingEdits
import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submission.sync.SaveRefusedException
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.EntranceDistance
import io.github.dostepnykampus.feature.capture.report.ReportSaver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * Model widoku C7 „Usunięcie bariery”. Zgłoszenie „bariera usunięta” wymaga nowego zdjęcia miejsca, a nie wymaga
 * uciążliwości; bez zdjęcia nic się nie zapisuje. Zapis idzie tą samą ścieżką co formularz C2 i czeka w telefonie
 * na wysłanie, powiązany z poprzednim zgłoszeniem. Własne zgłoszenie z kolejki telefonu otwarte z C3 zapisuje się
 * jako jego poprawka.
 *
 * Wymagania: US8.4, BR-19, BR-7, NFR-7, US8.3
 *
 * @param barriers zgłoszenia przyjęte, z których bierze się poprzednie
 * @param catalog katalog budynków, z którego są nazwy budynku i wejścia; bez niego miejsce ma sam symbol
 * @param nearest najbliższe wejście budynku bariery z katalogu
 * @param saver zapis w kolejce telefonu
 * @param edits zgłoszenia z kolejki telefonu do poprawki
 */
@HiltViewModel
class RemovedViewModel
    @Inject
    constructor(
        private val barriers: AcceptedBarriers,
        private val catalog: BuildingCatalogRepository,
        private val nearest: NearestEntrance,
        private val saver: ReportSaver,
        private val edits: PendingEdits,
    ) : ViewModel() {
        private val state = MutableStateFlow(RemovedUiState())
        private var requested: String? = null
        private var edited: EditedSubmission? = null

        /** Stan ekranu. */
        val uiState: StateFlow<RemovedUiState> = state.asStateFlow()

        /** Wczytuje zgłoszenie przyjęte [id]; kolejne wywołania nic nie zmieniają, więc obrót nie kasuje zdjęcia. */
        fun open(id: String) {
            if (requested != null) return
            requested = id
            viewModelScope.launch { show(previous = barriers.barriers().firstOrNull { it.id == id }, photo = null) }
        }

        /**
         * Wczytuje własne zgłoszenie [id] z kolejki telefonu z zapisanym zdjęciem; zapis zastąpi to zgłoszenie.
         * Kolejne wywołania nic nie zmieniają.
         *
         * Wymagania: US8.3, US8.4
         */
        fun openEdit(id: String) {
            if (requested != null) return
            requested = id
            viewModelScope.launch {
                // TODO(#4056): wysłanego zgłoszenia kolejka już nie ma, więc ekran wraca na C3.
                val own = edits.load(id)
                edited = own?.edited
                val replaced = own?.run { content.replacesSubmissionId }
                val previous = replaced?.let { accepted -> barriers.barriers().firstOrNull { it.id == accepted } }
                show(previous = previous, photo = own?.run { photos.firstOrNull() })
            }
        }

        /** Nowe zdjęcie miejsca; kolejne zastępuje poprzednie. Wymagania: US8.4, BR-19 */
        fun takePhoto(photo: PendingPhotoDraft) {
            state.update { if (it.isEditable) it.copy(photo = photo, isPhotoMissing = false) else it }
        }

        /** „Wyślij do sprawdzenia” i „Spróbuj ponownie”: bez zdjęcia zostaje błąd walidacji, ze zdjęciem zapis. */
        fun send() {
            val current = state.value
            val previous = current.previous
            val photo = current.photo
            if (!current.isEditable || previous == null) return
            if (photo == null) {
                state.value = current.copy(isPhotoMissing = true)
                return
            }
            state.value = current.copy(stage = RemovedStage.SAVING)
            viewModelScope.launch {
                val stage =
                    try {
                        saver.save(previous.removalContent(photo), listOf(photo), edited)
                        RemovedStage.SAVED
                    } catch (error: IOException) {
                        Timber.w("Zapis zgłoszenia usuniętej bariery nieudany: %s", error.javaClass.simpleName)
                        RemovedStage.SAVE_FAILED
                    } catch (error: SQLiteException) {
                        Timber.w("Zapis zgłoszenia usuniętej bariery nieudany: %s", error.javaClass.simpleName)
                        RemovedStage.SAVE_FAILED
                    } catch (_: SaveRefusedException) {
                        RemovedStage.SAVE_REJECTED
                    }
                state.update { it.copy(stage = stage) }
            }
        }

        private suspend fun show(previous: AcceptedBarrier?, photo: PendingPhotoDraft?) {
            val stage = if (previous == null) RemovedStage.MISSING else RemovedStage.EDITING
            val found = previous?.let { place(it) } ?: AcceptedPlace()
            state.update { current ->
                current.copy(
                    previous = previous,
                    buildingName = found.building,
                    entranceName = found.entrance,
                    nearest = found.nearest,
                    photo = photo ?: current.photo,
                    stage = stage,
                )
            }
        }

        private suspend fun place(barrier: AcceptedBarrier): AcceptedPlace? {
            val found = catalogOrNull() ?: return null
            val building = found.buildings.firstOrNull { it.symbol == barrier.buildingSymbol }
            val entrance = found.entrances.firstOrNull { it.id == barrier.entranceId }
            val closest = nearest.find(buildingSymbol = barrier.buildingSymbol, point = barrier.location)
            return AcceptedPlace(building = building?.name, entrance = entrance?.name, nearest = closest)
        }

        private suspend fun catalogOrNull(): BuildingCatalog? =
            try {
                catalog.catalog()
            } catch (error: IOException) {
                Timber.w("Katalog budynków dla ekranu usunięcia bariery niedostępny: %s", error.javaClass.simpleName)
                null
            } catch (error: SQLiteException) {
                Timber.w("Katalog budynków dla ekranu usunięcia bariery niedostępny: %s", error.javaClass.simpleName)
                null
            }
    }

/** Czeka, aż ekran ma się zamknąć: `true` po zapisie, `false`, gdy zgłoszenia nie ma. Wymagania: NFR-54 */
internal suspend fun RemovedViewModel.awaitExit(): Boolean =
    uiState.first { it.stage == RemovedStage.SAVED || it.stage == RemovedStage.MISSING }.stage == RemovedStage.SAVED

/**
 * Miejsce zgłoszenia przyjętego z katalogu budynków. Wymagania: US8.4
 *
 * @property building nazwa budynku
 * @property entrance nazwa wejścia przy barierze
 * @property nearest najbliższe wejście budynku z odległością
 */
internal data class AcceptedPlace(
    val building: String? = null,
    val entrance: String? = null,
    val nearest: EntranceDistance? = null,
)
