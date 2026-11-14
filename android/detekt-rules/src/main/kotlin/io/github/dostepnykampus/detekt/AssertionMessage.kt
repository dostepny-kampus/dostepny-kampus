/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Każda asercja ma komunikat. Wywołanie `assert…` albo `fail` z `org.junit.jupiter.api.Assertions` kończy się
 * parametrem `String` albo `Supplier`, a z `org.junit.Assert` zaczyna się parametrem `String`; przeciążenie wybiera
 * analiza typów, więc liczy się wywoływana sygnatura, nie liczba argumentów. `assertAll` tylko grupuje asercje,
 * więc komunikatu nie wymaga.
 */
class AssertionMessage(config: Config) :
    Rule(config, "Asercja bez komunikatu: dodaj opis oczekiwania jako ostatni argument (JUnit 4: pierwszy)."),
    RequiresAnalysisApi {
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (isAssertionName(expression.calleeExpression?.text) && lacksMessage(expression)) {
            report(Finding(Entity.from(expression), description))
        }
    }

    private fun isAssertionName(name: String?): Boolean =
        name != null && name != GROUPING && (name.startsWith("assert") || name == "fail")

    private fun lacksMessage(call: KtCallExpression): Boolean =
        analyze(call) {
            val symbol = call.resolveToCall()?.successfulFunctionCallOrNull()?.symbol
            val types = symbol?.valueParameters?.map { it.returnType }.orEmpty()
            when (symbol?.callableId?.classId) {
                JUPITER -> !hasJupiterMessage(symbol.callableId?.callableName?.asString(), types)
                JUNIT4 -> !isMessage(types.firstOrNull())
                else -> false
            }
        }

    private fun hasJupiterMessage(
        name: String?,
        types: List<KaType>,
    ): Boolean = if (name == "fail") types.any(::isMessage) else isMessage(types.lastOrNull())

    private fun isMessage(type: KaType?): Boolean {
        val bound = if (type is KaFlexibleType) type.lowerBound else type
        return (bound as? KaClassType)?.classId in MESSAGE_TYPES
    }

    private companion object {
        const val GROUPING = "assertAll"
        val JUPITER = ClassId.fromString("org/junit/jupiter/api/Assertions")
        val JUNIT4 = ClassId.fromString("org/junit/Assert")
        val MESSAGE_TYPES =
            setOf(
                ClassId.fromString("kotlin/String"),
                ClassId.fromString("java/lang/String"),
                ClassId.fromString("java/util/function/Supplier"),
            )
    }
}
