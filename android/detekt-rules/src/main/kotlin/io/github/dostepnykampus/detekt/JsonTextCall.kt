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
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Zakaz wywołań `decodeFromString`, `encodeToString` i `parseToJsonElement` poza plikami granicznymi tekstu JSON,
 * rozpoznawanych po nazwie, bez rozpoznawania typów. Uzupełnia regułę `ForbiddenMethodCall/tekst-json`, która
 * pomija wywołanie, gdy analiza nie wyznaczy jego typu, np. dla `serializer()` klasy `@Serializable` z modułu.
 * Wywołanie przez alias importu reguła przepuszcza.
 */
class JsonTextCall(config: Config) :
    Rule(
        config,
        "Tekst JSON poza plikiem granicznym. " +
            "Użyj typów GeoJSON z biblioteki albo przenieś zamianę do pliku granicznego.",
    ) {
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.calleeExpression?.text in NAMES) {
            report(Finding(Entity.from(expression), description))
        }
    }

    private companion object {
        val NAMES = setOf("decodeFromString", "encodeToString", "parseToJsonElement")
    }
}
