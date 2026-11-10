/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Przekodowanie zdjęcia z aparatu w zasięgu modelu widoku, więc obrót ekranu go nie przerywa, a gotowe zdjęcie
 * czeka na nową kompozycję (NFR-22, US4.8). Plik, którego nie dało się przekodować albo którego przekodowanie
 * przerwano razem z ekranem, jest usuwany, więc w pamięci aplikacji nie zostają pliki bez wpisu (NFR-50).
 *
 * @param workContext wątek przekodowania pliku
 * @param transcode przekodowanie pliku z aparatu; `false`, gdy plik nie jest obrazem
 */
@HiltViewModel
class PhotoTranscoder internal constructor(
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val transcode: (File) -> Boolean = ::shrinkPhoto,
) : ViewModel() {
    private val readyPhoto = MutableStateFlow<PendingPhotoDraft?>(null)
    private val lastFailed = MutableStateFlow(false)

    /** Zdjęcie gotowe do formularza, dopóki ekran go nie odbierze ([delivered]). */
    val ready: StateFlow<PendingPhotoDraft?> = readyPhoto

    /** `true`, gdy ostatniego zdjęcia nie dało się przekodować. */
    val failure: StateFlow<Boolean> = lastFailed

    /** Przekodowanie w aplikacji: [shrinkPhoto] poza wątkiem interfejsu. */
    @Inject
    constructor() : this(transcode = ::shrinkPhoto)

    /**
     * Przekodowuje zdjęcie [id] zapisane przez aparat w [file].
     *
     * @param id identyfikator zdjęcia w formularzu
     * @param file plik z aparatu
     */
    fun start(id: String, file: File) {
        lastFailed.value = false
        val job =
            viewModelScope.launch {
                val isReady = withContext(workContext) { file.transcodedBy(transcode) }
                if (isReady) {
                    readyPhoto.value = PendingPhotoDraft(photoId = id, path = file.path)
                } else {
                    file.delete()
                    lastFailed.value = true
                }
            }
        job.invokeOnCompletion { cause -> if (cause != null) file.delete() }
    }

    /** Ekran przekazał zdjęcie do formularza. */
    fun delivered() {
        readyPhoto.value = null
    }

    /** Nowa próba „Zrób zdjęcie” chowa komunikat o błędzie. */
    fun retry() {
        lastFailed.value = false
    }
}

internal fun File.transcodedBy(transcode: (File) -> Boolean): Boolean =
    try {
        transcode(this)
    } catch (_: IOException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    } catch (_: OutOfMemoryError) {
        false
    }
