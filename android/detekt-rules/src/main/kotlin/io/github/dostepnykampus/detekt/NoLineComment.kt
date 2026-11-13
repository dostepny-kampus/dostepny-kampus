/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.detekt

import com.intellij.psi.PsiComment
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens

/**
 * Zakaz komentarzy liniowych `//` w kodzie i skryptach Kotlina, na początku i na końcu linii z kodem:
 * opis idzie do KDoc deklaracji, której dotyczy. Komentarz rozpoznaje parser kompilatora, więc `//` w napisie,
 * także surowym albo z wieloma `$`, nie jest komentarzem. Komentarze blokowe i KDoc przechodzą. Wyjątkiem jest
 * znacznik pracy odłożonej z numerem issue i opisem, `TODO(#12): opis`: stoi przy wierszu, którego dotyczy.
 */
class NoLineComment(config: Config) :
    Rule(config, "Komentarz liniowy `//`: opis idzie do KDoc deklaracji, której dotyczy.") {
    override fun visitComment(comment: PsiComment) {
        super.visitComment(comment)
        if (comment.tokenType == KtTokens.EOL_COMMENT && !ISSUE_MARKER.matches(comment.text.trimEnd())) {
            report(Finding(Entity.from(comment), description))
        }
    }

    private companion object {
        val ISSUE_MARKER = Regex("""// TODO\(#\d+\): \S.*""")
    }
}
