/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import java.io.File

/**
 * Strażnik wyniku KSP: źródła z adnotacjami Hilt muszą dać pliki Java w katalogu generowanym przez KSP. Pusty wynik
 * zatrzymuje zadanie, więc nie trafia do cache budowania ani do testów (wcześniej test padał dopiero w czasie działania).
 */
object KspHiltGuard {
    private val markers = Regex("@(HiltViewModel|AndroidEntryPoint|HiltAndroidApp|HiltAndroidTest|InstallIn|EntryPoint)\\b")

    /** Błąd, gdy [sourceDirs] mają adnotacje Hilt, a [generated] nie ma żadnego pliku Java. */
    fun check(taskName: String, sourceDirs: List<File>, generated: File) {
        val withHilt = sourceDirs.filter { it.isDirectory }
            .flatMap { dir -> dir.walkTopDown().filter { it.extension == "kt" }.toList() }
            .any { markers.containsMatchIn(it.readText()) }
        val produced = generated.walkTopDown().any { it.isFile && it.extension == "java" }
        check(!withHilt || produced) {
            "Zadanie $taskName nie wygenerowało plików Java w $generated, choć źródła mają adnotacje Hilt: " +
                "pusty wynik KSP, którego nie wolno zapisać w cache ani użyć w testach."
        }
    }
}
