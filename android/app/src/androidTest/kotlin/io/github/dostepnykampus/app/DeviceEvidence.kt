/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream

private const val CHUNK_SIZE = 32_000

/** Zrzuty całego ekranu z IME trafiają do istniejącego artefaktu instrumentacja.txt, w numerowanych częściach PNG. */
internal object DeviceEvidence {
    fun shell(command: String): String =
        ParcelFileDescriptor
            .AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command))
            .bufferedReader()
            .use { it.readText() }

    fun record(label: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Brak zrzutu ekranu" }
        val encoded =
            try {
                ByteArrayOutputStream().use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Nie zapisano PNG" }
                    Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                }
            } finally {
                bitmap.recycle()
            }
        val parts = encoded.chunked(CHUNK_SIZE)
        parts.forEachIndexed { index, part ->
            val metadata = "${Build.MODEL}|${Build.VERSION.RELEASE}|${Build.VERSION.SDK_INT}"
            val line = "DK_SCREENSHOT|$metadata|$label|$index|${parts.size}|$part\n"
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", line) })
        }
    }
}
