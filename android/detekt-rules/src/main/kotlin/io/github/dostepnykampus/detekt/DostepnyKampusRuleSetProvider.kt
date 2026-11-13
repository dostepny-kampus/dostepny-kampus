/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/** Reguły detekt projektu; detekt wczytuje je z `detektPlugins` przez `ServiceLoader`. */
class DostepnyKampusRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("dostepnykampus")

    override fun instance(): RuleSet =
        RuleSet(
            ruleSetId,
            listOf(::NoLineComment, ::NoHistoryNumberInKDoc, ::KDocDescriptionLength, ::RequirementIdOutsideLine),
        )
}
