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
 * Zgłasza blok KDoc z numerem issue (krzyżyk i liczba) albo słowem „decyzja” z numerem, jeden raz na blok.
 * Znacznik pracy odłożonej z numerem issue nie jest zgłaszany. Słowo i numer rozdzielone złamaniem linii
 * też są zgłaszane.
 */
class NoHistoryNumberInKDoc(config: Config) :
    Rule(config, "KDoc z numerem issue albo decyzji: powód zmiany idzie do PR i issue, w kodzie zostaje kontrakt.") {
    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        file
            .collectDescendantsOfType<KDoc>()
            .filter { hasHistoryNumber(it.text) }
            .forEach { report(Finding(Entity.from(it), description)) }
    }

    private companion object {
        val DEFERRED_MARKER = Regex("""[T]ODO\(#\d+\)""")
        val LINE_BREAK = Regex("""\s*\n\s*\*?""")
        val ISSUE_NUMBER = Regex("""(?<![\w&/#])#\d{1,5}\b""")
        val DECISION_NUMBER = Regex("""(?i)\bdecyzj\w*\b[^.;()\d]{0,25}\d""")

        fun hasHistoryNumber(text: String): Boolean {
            val flat = text.replace(LINE_BREAK, " ").replace(DEFERRED_MARKER, "")
            return ISSUE_NUMBER.containsMatchIn(flat) || DECISION_NUMBER.containsMatchIn(flat)
        }
    }
}
