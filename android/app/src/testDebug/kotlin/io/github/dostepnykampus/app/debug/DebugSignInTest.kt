/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.account.AccountSession
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.auth.AccessTokens
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Base64

/** Sesja konta i token z grafu Hilt aplikacji testowej. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SessionEntryPoint {
    /** Sesja konta, ta sama co w aplikacji. */
    fun session(): AccountSession

    /** Token dołączany do żądań do serwera. */
    fun tokens(): AccessTokens
}

/** Przełącznik debug loguje użytkownika testowego roli i bierze rolę konta z tokenu. Wymagania: BR-1, NFR-35 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class DebugSignInTest {
    /** Graf Hilt aplikacji. */
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    /** Ekran testowy Compose. */
    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val server = MockWebServer().apply { start() }
    private val graph by lazy {
        EntryPointAccessors.fromApplication<SessionEntryPoint>(RuntimeEnvironment.getApplication())
    }
    private val session by lazy { graph.session() }
    private val tokens by lazy { graph.tokens() }
    private val signIn by lazy {
        DebugSignIn(session = session, http = OkHttpClient(), tokenUrl = server.url("/token").toString())
    }

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
        assertEquals(Account.SignedIn(setOf(StaffRole.VERIFIER)), session.account.value)
    }

    /** Koordynator: logowanie użytkownika `koordynator.test`, a konto z rolą z tokenu. */
    @Test
    fun coordinatorSignsIn() {
        val token = jwt("COORDINATOR", "offline_access")
        server.enqueue(MockResponse().setBody("""{"access_token":"$token","token_type":"Bearer"}"""))
        assertTrue("logowanie Koordynatora", runBlocking { signIn.choose(DebugRole.COORDINATOR) })
        val form = server.takeRequest().body.readUtf8()
        assertTrue("użytkownik testowy Koordynatora", form.contains("username=koordynator.test"))
        val expected = Account.SignedIn(setOf(StaffRole.COORDINATOR))
        assertEquals("konto z roli w tokenie", expected, session.account.value)
    }

    /** „Gość” usuwa token i wylogowuje. */
    @Test
    fun guestDropsToken() {
        server.enqueue(MockResponse().setBody("""{"access_token":"${jwt("VISITOR")}"}"""))
        runBlocking { signIn.choose(DebugRole.VISITOR) }
        assertTrue(runBlocking { signIn.choose(DebugRole.GUEST) })
        assertNull(tokens.token)
        assertEquals(Account.Guest, session.account.value)
    }

    /** Token, z którego nie da się odczytać konta, nie zmienia roli ani tokenu. */
    @Test
    fun unreadableTokenKeepsRole() {
        server.enqueue(MockResponse().setBody("""{"access_token":"nie-jwt"}"""))
        assertFalse(runBlocking { signIn.choose(DebugRole.COLLECTOR) })
        assertNull(tokens.token)
        assertEquals(Account.Guest, session.account.value)
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
        assertEquals(Account.SignedIn(), session.account.value)
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
