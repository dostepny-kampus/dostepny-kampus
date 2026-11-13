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
 * Zgłasza blok KDoc z identyfikatorem wymagania poza linią `Wymagania: …`, jeden raz na blok.
 * Linia `Wymagania:` kończy część opisową; w KDoc jednowierszowym stoi na końcu opisu.
 * Identyfikator w fragmencie kodu w odwrotnych apostrofach nie jest zgłaszany.
 */
class RequirementIdOutsideLine(config: Config) :
    Rule(config, "Identyfikator wymagania w zdaniu KDoc: przenieś go do linii `Wymagania:` na końcu opisu.") {
    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        file
            .collectDescendantsOfType<KDoc>()
            .filter { hasIdOutsideLine(it.text) }
            .forEach { report(Finding(Entity.from(it), description)) }
    }

    private companion object {
        val DECORATION = Regex("""^\s*(/\*\*|\*)?\s?""")
        val CODE_SPAN = Regex("`[^`]*`")
        val REQUIREMENTS = Regex("""(^|\s)Wymagania:[^\n]*$""")
        val REQUIREMENT_ID =
            Regex("""(?<![\w-])(?:US\d+(?:\.\d+)?|UC\d+|BR-G?\d+|NFR-\d+|RO-[A-Z]{3}|R-\d+|N\d+)(?![\w°-]|\.\d)""")

        fun hasIdOutsideLine(text: String): Boolean =
            text
                .removeSuffix("*/")
                .lines()
                .map { it.replace(DECORATION, "").replace(CODE_SPAN, "").replace(REQUIREMENTS, "") }
                .any { REQUIREMENT_ID.containsMatchIn(it) }
    }
}
