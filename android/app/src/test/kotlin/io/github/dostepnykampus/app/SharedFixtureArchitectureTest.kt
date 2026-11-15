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
    fun infra_test_own_data() {
        assertEquals(emptyList<String>(), violations(testScope()) - KNOWN.keys, HINT)
    }

    /** Każdy wpis listy wyjątków nadal jest naruszeniem. */
    @Test
    fun infra_stale_exceptions() {
        assertEquals(emptyList<String>(), KNOWN.keys.sorted() - violations(testScope()).toSet(), STALE)
    }

    /** Pole, `lateinit` i metody przygotowania łamią regułę; reguły, stałe i klasa bez testów nie. */
    @Test
    fun infra_rejects_shared_state(
        @TempDir fixtures: File,
    ) {
        File(fixtures, "Samples.kt").writeText(SAMPLES)
        assertEquals(
            listOf("BeforeAllTest", "BeforeClassTest", "BeforeEachTest", "BeforeTest", "FieldTest", "LateTest"),
            violations(Konsist.scopeFromExternalDirectory(fixtures.absolutePath)),
            "Reguła łapie pole, lateinit i metody przygotowania, a przepuszcza reguły i stałe",
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
        val normalized = path.replace(oldChar = '\\', newChar = '/')
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

        val SAMPLES =
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
            """.trimMargin()

        val TEST_SOURCE = Regex("""/src/(test\w*|androidTest\w*)/""")
        val TESTS = setOf("Test", "ParameterizedTest", "RepeatedTest", "TestFactory")
        val SETUPS = setOf("Before", "BeforeClass", "BeforeEach", "BeforeAll")
        val RULES = setOf("Rule", "ClassRule", "RegisterExtension")

        const val ISSUE = 1995

        /** Klasy testów ze wspólnym polem albo metodą przygotowania; issue, które to usuwa, to [ISSUE]. */
        val KNOWN =
            """
                |AcceptedBarriersCopyTest AccessTokenInterceptorTest AppNavigatorTest AppShellScreenshotTest
                |AppShellTest ArchitectureTest BarrierCacheDaoTest BarrierNamesTest BarrierSheetTest
                |BottomBarTest BuildingCacheDaoTest BuildingCardModesTest BuildingCardOfflineTest BuildingCardTest
                |BuildingCatalogCacheTest BuildingCatalogParserTest BuildingCatalogRepositoryTest
                |BuildingListTest BuildingOnMapCameraTest BuildingsApiTest CampusAreaEdgeDeviceTest CampusAreasPwrTest
                |CampusBoundsTest CampusMapBarriersStyleTest CampusMapDataTest CampusMapEntrancesStyleTest
                |CampusMapGeometryTest CampusMapScreenTest CampusMapSelectionStyleTest CampusMapStyleTest
                |CampusMapViewModelTest CampusPathDeviceTest CancellationTest
                |CardServerEntrancesTest CollectorReachTest ConflictEditTest DebugRolesTest DebugSignInTest
                |DeviceStepsTest DtoContractTest EditBaseVersionTest EditDuringSendTest EntranceAssessmentTest
                |EntranceHiltTest EntranceOnMapTest FirstLaunchRepositoryTest
                |FirstLaunchViewModelTest FloorMarkersTest FloorPickerTest
                |GuestSignInTest HelpScreenTest
                |ListEntryTest
                |LocationPermissionTest LoginExpiredGateTest MainActivityTest MapAreaPanelTest
                |MapAreaSwitchTest MapAreasTest MapBarrierNamesTest MapBarrierTouchTest MapBarriersLoadTest
                |MapCameraTest MapCatalogFromFileTest MapControlsTest MapGeometryFlowTest MapGeometryFocusTest
                |MapGesturesTest MapLimitsTest MapListChoiceRepositoryTest MapOverlayNoticeTest
                |MapSceneStateTest MapSelectionImagesTest MapStreamConcurrencyTest MapStyleRememberTest MapTileHttpTest
                |MapTileNetworkDeviceTest MeasurePathTest MeasureScreenTest MeasureViewModelTest MeasuredPlaceTest
                |MeasuredReportTest MigrationTest MySubmissionsScreenTest MySubmissionsViewModelTest
                |NullabilityArchitectureTest
                |OrphanPhotosTest OwnHistoryTest OwnListPathTest OwnListTest OwnStatusesTest OwnSubmissionDaoTest
                |PendingEditMappingTest PendingEditsTest PendingMarkerChangeDaoTest PendingSubmissionDaoTest
                |PermissionsTest PhotoCameraTest PhotoFilesTest PlaceSectionTest
                |QueueAfterRestartTest QueueTransactionTest QuickOfflineTest QuickReportRouteTest QuickReportScreenTest
                |QuickReportTest QuickSendStepTest QuickSendViewModelTest RefusedResaveTest RefusedSubmissionTest
                |RemovedViewModelTest ReportAtEntranceTest ReportDraftTest ReportEditScreenTest ReportEditTest
                |ReportEditViewModelTest ReportFloorsTest ReportFormScreenTest ReportFormViewModelTest
                |ReportMissingFieldTest ReportScreenTest RevokedRoleTest SaveDuringSendTest
                |SavedReportTest SearchOtherAreaTest SelectedEntranceTest ServerBuildingCatalogTest
                |SheetBackGestureTest ShowEntranceTest SubmissionCardTest SubmissionFlagsTest SubmissionJsonTest
                |SubmissionPhotoReadTest SubmissionQueueTest SubmissionSyncAtStartTest SubmissionSyncWorkerTest
                |SubmissionsApiTest SubmissionsContractTest TestViewModelsTest TokensTest
                |UnreadableSubmissionTest
            """.trimMargin()
                .lines()
                .flatMap { it.split(" ") }
                .associateWith { ISSUE }
    }
}
