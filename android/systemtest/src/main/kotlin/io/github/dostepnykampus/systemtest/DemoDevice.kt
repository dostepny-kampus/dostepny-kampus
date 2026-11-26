/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.systemtest

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.core.content.IntentCompat
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.EntryPointAccessors
import io.github.dostepnykampus.app.debug.DebugRole
import io.github.dostepnykampus.app.debug.DebugRolesEntryPoint
import io.github.dostepnykampus.core.testing.DeviceSteps
import kotlinx.coroutines.runBlocking

/**
 * Telefon w drodze demo: rola z przełącznika debug, pozycje paska dolnego (napis ze spacjami niełamiącymi
 * po krótkich słowach), przewijanie do węzła, sieć emulatora i aparat, który zamiast aplikacji aparatu zapisuje
 * szare zdjęcie. Wyłączoną sieć przywraca [network], także gdy test padnie, bo następne testy tego samego
 * przebiegu potrzebują sieci.
 *
 * Wymagania: US6.1, US9.2, US14.1
 *
 * @param compose reguła testu z aktywnością aplikacji
 * @param steps kroki wspólne testów na urządzeniu
 */
internal class DemoDevice(private val compose: ComposeTestRule, private val steps: DeviceSteps) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext

    /** Zgoda na dokładną lokalizację dla pomiaru C1, bez okna systemu. */
    fun grantLocation() {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).forEach {
            instrumentation.uiAutomation.grantRuntimePermission(app.packageName, it)
        }
    }

    /** Wraca do mapy V1 i loguje użytkownika testowego [role] tak jak przełącznik ról debug. */
    fun signIn(role: DebugRole) {
        toMap()
        val signIn = EntryPointAccessors.fromApplication(app, DebugRolesEntryPoint::class.java).signIn()
        check(runBlocking { signIn.choose(role) }) { "Logowanie roli $role" }
    }

    /** Otwiera z „Konto i praca” czynność [action], np. „Moje zgłoszenia”. */
    fun openWork(action: String) {
        steps.waitFor(spaced(ACCOUNT_AND_WORK))
        compose.onAllNodes(spaced(ACCOUNT_AND_WORK)).onFirst().performClick()
        reveal(spaced(action)).performClick()
    }

    /** Pierwszy węzeł [matcher] przewinięty na ekran; lista leniwa przewija się do niego, zanim go złoży. */
    fun reveal(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        if (compose.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()) {
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(matcher)
        }
        val scrolled = matcher and hasAnyAncestor(hasScrollAction())
        if (compose.onAllNodes(scrolled).fetchSemanticsNodes().isNotEmpty()) {
            compose.onAllNodes(scrolled).onFirst().performScrollTo()
        }
        return compose.onAllNodes(matcher).onFirst()
    }

    /** Włącza albo wyłącza Wi-Fi i dane komórkowe emulatora poleceniem `svc` z uprawnieniami powłoki. */
    fun network(isOn: Boolean) {
        val state = if (isOn) "enable" else "disable"
        listOf("wifi", "data").forEach { radio -> shell("svc $radio $state") }
    }

    /**
     * Aparat w „Zrób zdjęcie”: intencja `ACTION_IMAGE_CAPTURE` nie wychodzi z aplikacji, a odpowiedź zapisuje
     * szare zdjęcie JPEG pod adresem z `EXTRA_OUTPUT` i zwraca „zrobione”. Wymaga [Intents.init].
     */
    fun stubCamera() {
        Intents.intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWithFunction { intent ->
            val target = checkNotNull(outputOf(intent)) { "Intencja aparatu bez EXTRA_OUTPUT" }
            app.contentResolver.openOutputStream(target).use { out -> checkNotNull(out).write(grayJpeg()) }
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        }
    }

    private fun toMap() {
        var backs = 0
        while (compose.onAllNodes(hasText(MAP)).fetchSemanticsNodes().isEmpty() && backs < MAX_BACKS) {
            Espresso.pressBack()
            compose.waitForIdle()
            backs++
        }
        compose.onAllNodes(hasText(MAP)).onFirst().performClick()
    }

    private fun spaced(text: String): SemanticsMatcher =
        SemanticsMatcher("napis „$text” ze spacjami niełamiącymi albo bez") { node ->
            node.config
                .getOrNull(SemanticsProperties.Text)
                .orEmpty()
                .any { shown -> shown.text.replace(NO_BREAK_SPACE, ' ') == text }
        }

    private fun shell(command: String) {
        ParcelFileDescriptor
            .AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .use { it.readBytes() }
    }

    private fun outputOf(intent: Intent): Uri? =
        IntentCompat.getParcelableExtra(intent, MediaStore.EXTRA_OUTPUT, Uri::class.java)

    private companion object {
        const val MAP = "Mapa"
        const val MAX_BACKS = 4
        const val ACCOUNT_AND_WORK = "Konto i praca"
        const val NO_BREAK_SPACE = '\u00A0'
    }
}
