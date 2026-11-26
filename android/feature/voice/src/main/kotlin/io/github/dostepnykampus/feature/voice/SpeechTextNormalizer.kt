/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.voice

import java.text.Normalizer

/**
 * Normalizuje [text] z rozpoznawania mowy do jednolitej postaci porównawczej.
 * Zamienia wielkie litery na małe, składa polskie znaki do liter ASCII, usuwa interpunkcję
 * z zachowaniem minusa przed cyfrą oraz zastępuje polskie liczebniki słowne cyframi.
 */
public fun normalizeSpeechText(text: String): String {
    val cleaned =
        PUNCTUATION
            .replace(input = foldDiacritics(text), replacement = " ")
            .trim()
    return if (cleaned.isEmpty()) "" else combineNumerals(cleaned.split(WHITESPACE))
}

private fun foldDiacritics(text: String): String =
    COMBINING_MARKS
        .replace(input = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD), replacement = "")
        .replace(oldChar = 'ł', newChar = 'l')

private fun combineNumerals(tokens: List<String>): String {
    val result = mutableListOf<String>()
    var pendingTotal: Int? = null
    var allowedNextRank = 0
    for (token in tokens) {
        val numeral = NUMERALS[token]
        if (numeral == null) {
            pendingTotal?.let { result.add(it.toString()) }
            pendingTotal = null
            allowedNextRank = 0
            result.add(token)
        } else if (pendingTotal != null && numeral.rank > 0 && numeral.rank <= allowedNextRank) {
            pendingTotal += numeral.value
            allowedNextRank = numeral.nextRank
        } else {
            pendingTotal?.let { result.add(it.toString()) }
            pendingTotal = numeral.value
            allowedNextRank = numeral.nextRank
        }
    }
    pendingTotal?.let { result.add(it.toString()) }
    return result.joinToString(separator = " ")
}

private fun MutableMap<String, Numeral>.addWordGroup(
    forms: List<String>,
    ordinalStem: String,
    endings: List<String>,
    numeral: Numeral,
) {
    forms.forEach { put(it, numeral) }
    endings.forEach { put(ordinalStem + it, numeral) }
}

private data class Numeral(val value: Int, val rank: Int, val nextRank: Int)

private data class WordDef(val forms: List<String>, val stem: String, val endings: List<String> = HARD_ENDINGS)

private const val TEN = 10
private const val HUNDRED = 100
private const val THOUSAND = 1000
private const val RANK_THOUSAND = 4
private const val RANK_HUNDRED = 3
private const val RANK_TEN = 2
private const val RANK_UNIT = 1

private val COMBINING_MARKS = Regex("""\p{M}+""")
private val PUNCTUATION = Regex("""(?:(?<=\S)-|-(?!\d)|[^\p{L}\p{N}\s-])""")
private val WHITESPACE = Regex("""\s+""")

private val HARD_ENDINGS = listOf("y", "a", "e", "ego", "ej", "emu", "ym", "ymi")
private val SOFT_ENDINGS = listOf("i", "a", "ie", "iego", "iej", "iemu", "im", "imi")
private val THIRD_ENDINGS = listOf("", "a", "e", "ego", "ej", "emu", "m", "mi")

private val UNIT_DEFS =
    listOf(
        WordDef(listOf("jeden", "jedna", "jedno", "jednego", "jednej", "jednemu", "jednym"), "pierwsz"),
        WordDef(listOf("dwa", "dwie", "dwoch", "dwu", "dwom", "dwoma", "dwiema"), "drug", SOFT_ENDINGS),
        WordDef(listOf("trzy", "trzech", "trzem", "trzema"), "trzeci", THIRD_ENDINGS),
        WordDef(listOf("cztery", "czterech", "czterem", "czterema"), "czwart"),
        WordDef(listOf("piec", "pieciu", "piecioma"), "piat"),
        WordDef(listOf("szesc", "szesciu", "szescioma"), "szost"),
        WordDef(listOf("siedem", "siedmiu", "siedmioma"), "siodm"),
        WordDef(listOf("osiem", "osmiu", "osmioma"), "osm"),
        WordDef(listOf("dziewiec", "dziewieciu", "dziewiecioma"), "dziewiat"),
    )

private val TEEN_DEFS =
    listOf(
        WordDef(listOf("dziesiec", "dziesieciu", "dziesiecioma"), "dziesiat"),
        WordDef(listOf("jedenascie", "jedenastu", "jedenastoma"), "jedenast"),
        WordDef(listOf("dwanascie", "dwunastu", "dwunastoma"), "dwunast"),
        WordDef(listOf("trzynascie", "trzynastu", "trzynastoma"), "trzynast"),
        WordDef(listOf("czternascie", "czternastu", "czternastoma"), "czternast"),
        WordDef(listOf("pietnascie", "pietnastu", "pietnastoma"), "pietnast"),
        WordDef(listOf("szesnascie", "szesnastu", "szesnastoma"), "szesnast"),
        WordDef(listOf("siedemnascie", "siedemnastu", "siedemnastoma"), "siedemnast"),
        WordDef(listOf("osiemnascie", "osiemnastu", "osiemnastoma"), "osiemnast"),
        WordDef(listOf("dziewietnascie", "dziewietnastu", "dziewietnastoma"), "dziewietnast"),
    )

private val TEN_DEFS =
    listOf(
        WordDef(listOf("dwadziescia", "dwudziestu", "dwudziestoma"), "dwudziest"),
        WordDef(listOf("trzydziesci", "trzydziestu", "trzydziestoma"), "trzydziest"),
        WordDef(listOf("czterdziesci", "czterdziestu", "czterdziestoma"), "czterdziest"),
        WordDef(listOf("piecdziesiat", "piecdziesieciu", "piecdziesiecioma"), "piecdziesiat"),
        WordDef(listOf("szescdziesiat", "szescdziesieciu", "szescdziesiecioma"), "szescdziesiat"),
        WordDef(listOf("siedemdziesiat", "siedemdziesieciu", "siedemdziesiecioma"), "siedemdziesiat"),
        WordDef(listOf("osiemdziesiat", "osiemdziesieciu", "osiemdziesiecioma"), "osiemdziesiat"),
        WordDef(listOf("dziewiecdziesiat", "dziewiecdziesieciu", "dziewiecdziesiecioma"), "dziewiecdziesiat"),
    )

private val HUNDRED_DEFS =
    listOf(
        WordDef(listOf("sto", "stu", "stoma"), "setn"),
        WordDef(listOf("dwiescie", "dwustu", "dwustoma"), "dwusetn"),
        WordDef(listOf("trzysta", "trzystu", "trzystoma"), "trzysetn"),
        WordDef(listOf("czterysta", "czterystu", "czterystoma"), "czterysetn"),
        WordDef(listOf("piecset", "pieciuset", "pieciusetoma"), "piecsetn"),
        WordDef(listOf("szescset", "szesciuset", "szesciusetoma"), "szescsetn"),
        WordDef(listOf("siedemset", "siedmiuset", "siedmiusetoma"), "siedemsetn"),
        WordDef(listOf("osiemset", "osmiuset", "osmiusetoma"), "osiemsetn"),
        WordDef(listOf("dziewiecset", "dziewieciuset", "dziewieciusetoma"), "dziewiecsetn"),
    )

private val NUMERALS: Map<String, Numeral> =
    buildMap {
        addWordGroup(
            forms = listOf("zero"),
            ordinalStem = "zerow",
            endings = HARD_ENDINGS,
            numeral = Numeral(value = 0, rank = 0, nextRank = 0),
        )
        UNIT_DEFS.forEachIndexed { index, (forms, stem, endings) ->
            addWordGroup(
                forms = forms,
                ordinalStem = stem,
                endings = endings,
                numeral = Numeral(value = index + 1, rank = RANK_UNIT, nextRank = 0),
            )
        }
        TEEN_DEFS.forEachIndexed { index, (forms, stem) ->
            addWordGroup(
                forms = forms,
                ordinalStem = stem,
                endings = HARD_ENDINGS,
                numeral = Numeral(value = TEN + index, rank = RANK_TEN, nextRank = 0),
            )
        }
        TEN_DEFS.forEachIndexed { index, (forms, stem) ->
            addWordGroup(
                forms = forms,
                ordinalStem = stem,
                endings = HARD_ENDINGS,
                numeral = Numeral(value = (index + 2) * TEN, rank = RANK_TEN, nextRank = RANK_UNIT),
            )
        }
        HUNDRED_DEFS.forEachIndexed { index, (forms, stem) ->
            addWordGroup(
                forms = forms,
                ordinalStem = stem,
                endings = HARD_ENDINGS,
                numeral = Numeral(value = (index + 1) * HUNDRED, rank = RANK_HUNDRED, nextRank = RANK_TEN),
            )
        }
        addWordGroup(
            forms = listOf("tysiac", "tysiaca", "tysiacu", "tysiacem", "tysiecy"),
            ordinalStem = "tysieczn",
            endings = HARD_ENDINGS,
            numeral = Numeral(value = THOUSAND, rank = RANK_THOUSAND, nextRank = RANK_HUNDRED),
        )
    }
