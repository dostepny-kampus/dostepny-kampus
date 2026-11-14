/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Test przygotowuje swoje dane sam: klasa z metodami testów w `src/test*` i `src/androidTest*` nie ma
 * właściwości w ciele ani metod `@Before`, `@BeforeClass`, `@BeforeEach` i `@BeforeAll`. Dozwolone są
 * właściwości z adnotacją reguły (`@Rule`, `@ClassRule`, `@RegisterExtension`), czyli infrastruktura
 * uruchamiania testu, oraz niezmienne stałe w `companion object`. Klasy z dzisiejszymi naruszeniami
 * są na liście wyjątków z numerem issue; lista tylko maleje.
 */
class SharedFixtureArchitectureTest {
    /** Klasy testów spoza listy wyjątków nie mają wspólnych pól ani metod przygotowania. */
    @Test
    fun infra_test_prepares_own_data() {
        assertEquals(emptyList<String>(), violations(testScope()) - KNOWN.keys, HINT)
    }

    /** Każdy wpis listy wyjątków nadal jest naruszeniem. */
    @Test
    fun infra_exception_list_holds_only_violations() {
        assertEquals(emptyList<String>(), KNOWN.keys.sorted() - violations(testScope()).toSet(), STALE)
    }

    /** Pole, `lateinit` i metody przygotowania łamią regułę; reguły, stałe i klasa bez testów nie. */
    @Test
    fun infra_rule_rejects_shared_fixture(
        @TempDir fixtures: File,
    ) {
        File(fixtures, "Samples.kt").writeText(
            """
                |package sample
                |
                |class FieldTest {
                |    private val name = "a"
                |
                |    @Test
                |    fun check() = Unit
                |}
                |
                |class LateTest {
                |    private lateinit var name: String
                |
                |    @Test
                |    fun check() = Unit
                |}
                |
                |class BeforeTest {
                |    @Before
                |    fun prepare() = Unit
                |
                |    @Test
                |    fun check() = Unit
                |}
                |
                |class BeforeEachTest {
                |    @BeforeEach
                |    fun prepare() = Unit
                |
                |    @ParameterizedTest
                |    fun check() = Unit
                |}
                |
                |class BeforeClassTest {
                |    companion object {
                |        @JvmStatic
                |        @BeforeClass
                |        fun prepare() = Unit
                |    }
                |
                |    @Test
                |    fun check() = Unit
                |}
                |
                |class BeforeAllTest {
                |    @BeforeAll
                |    fun prepare() = Unit
                |
                |    @Test
                |    fun check() = Unit
                |}
                |
                |class RuleTest {
                |    @get:Rule
                |    val compose = Any()
                |
                |    @JvmField
                |    @Rule
                |    val hilt = Any()
                |
                |    @get:ClassRule
                |    val shared = Any()
                |
                |    @RegisterExtension
                |    val extension = Any()
                |
                |    @Test
                |    fun check() = Unit
                |
                |    companion object {
                |        const val LIMIT = 1
                |        val NAMES = listOf("a")
                |    }
                |}
                |
                |class HelperHolder {
                |    private val name = "a"
                |}
                |
            """.trimMargin(),
        )
        assertEquals(
            listOf("BeforeAllTest", "BeforeEachTest", "BeforeTest", "FieldTest", "LateTest"),
            violations(Konsist.scopeFromExternalDirectory(fixtures.absolutePath)),
        )
    }

    private fun testScope(): KoScope = Konsist.scopeFromProject()

    private fun violations(scope: KoScope): List<String> =
        scope
            .classes()
            .asSequence()
            .filter { isTestPath(it.path) && hasTests(it) && (hasFixtureProperty(it) || hasSetup(it)) }
            .map { it.name }
            .distinct()
            .sorted()
            .toList()

    private fun isTestPath(path: String): Boolean {
        val normalized = path.replace(oldChar = '\', newChar = '/')
        return TEST_SOURCE.containsMatchIn(normalized) || !normalized.contains("/src/")
    }

    private fun hasTests(type: KoClassDeclaration): Boolean =
        type.functions(includeNested = false).any { fn -> fn.annotations.any { it.name in TESTS } }

    private fun hasFixtureProperty(type: KoClassDeclaration): Boolean =
        type.properties(includeNested = false).any { property -> property.annotations.none { it.name in RULES } }

    private fun hasSetup(type: KoClassDeclaration): Boolean =
        (type.functions(includeNested = false) + type.objects(includeNested = false).flatMap { it.functions() })
            .any { fn -> fn.annotations.any { it.name in SETUPS } }

    private companion object {
        const val HINT = "Klasa testu ma wspólne pole albo metodę przygotowania; dane przygotuj w teście"
        const val STALE = "Wyjątek z listy już nie jest naruszeniem; usuń wpis"

        val TEST_SOURCE = Regex("""/src/(test\w*|androidTest\w*)/""")
        val TESTS = setOf("Test", "ParameterizedTest", "RepeatedTest", "TestFactory")
        val SETUPS = setOf("Before", "BeforeClass", "BeforeEach", "BeforeAll")
        val RULES = setOf("Rule", "ClassRule", "RegisterExtension")

        /** Klasy testów ze wspólnym polem albo metodą przygotowania → issue, które to usuwa. */
        val KNOWN = emptyMap<String, Int>()
    }
}
