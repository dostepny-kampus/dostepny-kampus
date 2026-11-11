/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

package io.github.dostepnykampus.app.debug

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.AccessTokens
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64

/** Przełącznik debug loguje użytkownika testowego roli i bierze rolę konta z tokenu (BR-1, NFR-35). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class DebugSignInTest {
    /** Ekran testowy Compose. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val server = MockWebServer().apply { start() }
    private val roles = DebugRoles()
    private val tokens = AccessTokens()
    private val signIn =
        DebugSignIn(
            roles = roles,
            tokens = tokens,
            http = OkHttpClient(),
            tokenUrl = server.url("/token").toString(),
        )

    /** Zamyka fałszywy serwer tokenów. */
    @After
    fun stop() {
        server.shutdown()
    }

    /** Zbierający: logowanie jego użytkownika testowego, a rola konta z tokenu, nie z przycisku. */
    @Test
    fun roleFromToken() {
        val token = jwt("VERIFIER", "offline_access")
        server.enqueue(MockResponse().setBody("""{"access_token":"$token","token_type":"Bearer"}"""))
        assertTrue(runBlocking { signIn.choose(DebugRole.COLLECTOR) })
        val form = server.takeRequest().body.readUtf8()
        assertTrue(form.contains("username=zbierajacy.test"))
        assertTrue(form.contains("client_id=dostepny-kampus-android"))
        assertEquals(token, tokens.token)
        assertEquals(Account.SignedIn(setOf(StaffRole.VERIFIER)), roles.account)
    }

    /** „Gość” usuwa token i wylogowuje. */
    @Test
    fun guestDropsToken() {
        server.enqueue(MockResponse().setBody("""{"access_token":"${jwt("VISITOR")}"}"""))
        runBlocking { signIn.choose(DebugRole.VISITOR) }
        assertTrue(runBlocking { signIn.choose(DebugRole.GUEST) })
        assertNull(tokens.token)
        assertEquals(Account.Guest, roles.account)
    }

    /** Błąd serwera tokenów nie zmienia roli ani tokenu. */
    @Test
    fun failureKeepsRole() {
        server.enqueue(MockResponse().setBody("""{"access_token":"${jwt("VISITOR")}"}"""))
        runBlocking { signIn.choose(DebugRole.VISITOR) }
        val before = tokens.token
        server.enqueue(MockResponse().setResponseCode(UNAUTHORIZED).setBody("""{"error":"invalid_grant"}"""))
        assertFalse(runBlocking { signIn.choose(DebugRole.VERIFIER) })
        assertEquals(before, tokens.token)
        assertEquals(Account.SignedIn(), roles.account)
    }

    /** Po błędzie logowania przełącznik pokazuje komunikat. */
    @Test
    fun failureShowsMessage() {
        compose.setContent {
            DostepnyKampusTheme { RoleSwitch(current = Account.Guest, onChoose = {}, isFailed = true) }
        }
        compose.onNodeWithText("Nie udało się zalogować", substring = true).assertIsDisplayed()
    }

    private fun jwt(vararg realmRoles: String): String {
        val list = realmRoles.joinToString(separator = ",") { "\"$it\"" }
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"RS256"}""".toByteArray())
        val payload = encoder.encodeToString("""{"realm_access":{"roles":[$list]}}""".toByteArray())
        return "$header.$payload.podpis"
    }

    private companion object {
        const val UNAUTHORIZED = 401
    }
}
