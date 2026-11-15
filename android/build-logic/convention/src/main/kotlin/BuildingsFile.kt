/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

/**
 * Ścieżka pliku danych dla testu we właściwości systemowej [property] (domyślnie `dostepnykampus.buildings`, plik
 * konturów); wejściem zadania jest treść pliku, a nie jego położenie na dysku.
 */
abstract class BuildingsFile : CommandLineArgumentProvider {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val file: RegularFileProperty

    @get:Input
    abstract val property: Property<String>

    init {
        property.convention("dostepnykampus.buildings")
    }

    override fun asArguments(): List<String> = listOf("-D${property.get()}=${file.get().asFile.path}")
}
