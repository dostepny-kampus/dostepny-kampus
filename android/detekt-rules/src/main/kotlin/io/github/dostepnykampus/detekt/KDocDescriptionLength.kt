/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Zgłasza blok KDoc, którego część opisowa ma więcej niż [MAX_LINES] niepustych linii.
 * Część opisowa kończy się na pierwszej linii zaczynającej się znacznikiem `@`; linie znaczników nie są liczone.
 */
class KDocDescriptionLength(config: Config) :
    Rule(config, "Opis KDoc dłuższy niż $MAX_LINES linii: zostaw kontrakt, wymagania są w dokumentacji wymagań.") {
    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        file
            .collectDescendantsOfType<KDoc>()
            .filter { descriptionLines(it.text) > MAX_LINES }
            .forEach { report(Finding(Entity.from(it), description)) }
    }

    private companion object {
        const val MAX_LINES = 6

        val DECORATION = Regex("""^\s*(/\*\*|\*/|\*)?\s?""")

        fun descriptionLines(text: String): Int =
            text
                .removeSuffix("*/")
                .lines()
                .map { it.replace(DECORATION, "") }
                .takeWhile { !it.startsWith("@") }
                .count { it.isNotBlank() }
    }
}
