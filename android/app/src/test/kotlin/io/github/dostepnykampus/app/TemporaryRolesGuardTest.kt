/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Strażnik adnotacji `TymczasoweRole`: rola bez logowania jest tymczasowa, więc gdy w kodzie pojawi się logowanie
 * (AppAuth albo repozytorium logowania), adnotacja ma zniknąć, a kompilator wskaże jej użycia (ADR-0003).
 */
class TemporaryRolesGuardTest {
    @Test
    fun `bez logowania adnotacja jest`() {
        assertEquals(emptySet<String>(), leftovers(Konsist.scopeFromProduction().files))
    }

    @Test
    fun `logowanie i adnotacja to błąd`(
        @TempDir fixtures: File,
    ) {
        val files =
            fixtures.fixture(name = "TymczasoweRole.kt", source = "package $ACCOUNT\n\n$ANNOTATION") +
                fixtures.fixture(
                    name = "LoginRepository.kt",
                    source =
                        listOf("package $ACCOUNT", "import net.openid.appauth.AuthState", "class LoginRepository")
                            .joinToString(separator = "\n\n"),
                )
        assertEquals(setOf("TymczasoweRole.kt"), leftovers(files))
    }

    @Test
    fun `sama adnotacja przechodzi`(
        @TempDir fixtures: File,
    ) {
        val files = fixtures.fixture(name = "TymczasoweRole.kt", source = "package $ACCOUNT\n\n$ANNOTATION")
        assertEquals(emptySet<String>(), leftovers(files))
    }

    // Pliki z adnotacją albo jej użyciem, ale tylko wtedy, gdy w kodzie jest już logowanie.
    private fun leftovers(files: List<KoFileDeclaration>): Set<String> {
        val hasLogin = files.any { file -> file.hasLogin() }
        val marked = files.filter { it.text.contains(MARKER) }.map { it.nameWithExtension }
        return if (hasLogin) marked.toSet() else emptySet()
    }

    private fun KoFileDeclaration.hasLogin(): Boolean =
        imports.any { it.name.startsWith(APP_AUTH) } || classes().any { LOGIN_CLASS.matches(it.name) }

    private fun File.fixture(name: String, source: String): List<KoFileDeclaration> {
        val directory = File(this, name.substringBefore('.')).apply { mkdirs() }
        File(directory, name).writeText(source)
        return Konsist.scopeFromExternalDirectory(directory.absolutePath).files
    }

    private companion object {
        const val ACCOUNT = "io.github.dostepnykampus.core.data.account"
        const val MARKER = "TymczasoweRole"
        const val APP_AUTH = "net.openid.appauth"
        const val ANNOTATION = "@RequiresOptIn\nannotation class TymczasoweRole"
        val LOGIN_CLASS = Regex(".*(Login|Auth)Repository")
    }
}
