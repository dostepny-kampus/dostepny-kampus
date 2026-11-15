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
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submission.sync.SaveRefusedException
import io.github.dostepnykampus.feature.capture.report.form.ReportSaver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * Model widoku C7 „Usunięcie bariery”. Zgłoszenie „bariera usunięta” wymaga nowego zdjęcia miejsca, a nie wymaga
 * uciążliwości; bez zdjęcia nic się nie zapisuje. Zapis idzie tą samą ścieżką co formularz C2 i czeka w telefonie
 * na wysłanie, powiązany z poprzednim zgłoszeniem.
 *
 * Wymagania: US8.4, BR-19, BR-7, NFR-7
 *
 * @param barriers zgłoszenia przyjęte, z których bierze się poprzednie
 * @param saver zapis w kolejce telefonu
 */
@HiltViewModel
class RemovedViewModel
    @Inject
    constructor(
        private val barriers: AcceptedBarriers,
        private val saver: ReportSaver,
    ) : ViewModel() {
        private val state = MutableStateFlow(RemovedUiState())
        private var requested: String? = null

        /** Stan ekranu. */
        val uiState: StateFlow<RemovedUiState> = state.asStateFlow()

        /** Wczytuje zgłoszenie przyjęte [id]; kolejne wywołania nic nie zmieniają, więc obrót nie kasuje zdjęcia. */
        fun open(id: String) {
            if (requested != null) return
            requested = id
            viewModelScope.launch {
                val previous = barriers.barriers().firstOrNull { it.id == id }
                val stage = if (previous == null) RemovedStage.MISSING else RemovedStage.EDITING
                state.update { it.copy(previous = previous, stage = stage) }
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
                        saver.save(previous.removalContent(photo), listOf(photo), null)
                        RemovedStage.SAVED
                    } catch (_: IOException) {
                        RemovedStage.SAVE_FAILED
                    } catch (_: SQLiteException) {
                        RemovedStage.SAVE_FAILED
                    } catch (_: SaveRefusedException) {
                        RemovedStage.SAVE_REJECTED
                    }
                state.update { it.copy(stage = stage) }
            }
        }
    }
