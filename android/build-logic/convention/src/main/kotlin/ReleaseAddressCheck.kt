/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Zatrzymuje budowę pliku release, gdy któryś adres z [addresses] (nazwa [ServerAddress] → adres, pusty przy braku)
 * nie jest poprawnym adresem `https://`.
 */
@DisableCachingByDefault(because = "Sprawdzenie nie ma wyniku do zapisania")
abstract class ReleaseAddressCheck : DefaultTask() {
    /** Adresy release według nazwy [ServerAddress]; pusty tekst, gdy adresu nie ustawiono. */
    @get:Input
    abstract val addresses: MapProperty<String, String>

    /** Rzuca [GradleException] z listą wszystkich błędnych adresów. */
    @TaskAction
    fun check() {
        val problems = addresses.get().mapNotNull { (name, url) -> ServerAddress.valueOf(name).problem(url) }
        if (problems.isNotEmpty()) {
            throw GradleException(problems.joinToString(separator = "\n", prefix = "Wersja release bez adresów:\n"))
        }
    }
}
