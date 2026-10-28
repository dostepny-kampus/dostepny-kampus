import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings

private const val M3 = "androidx.compose.material3"
private const val UNIT = "androidx.compose.ui.unit"
private val COLOR = ClassName("androidx.compose.ui.graphics", "Color")
private val DP = MemberName(UNIT, "dp", isExtension = true)
private val THEMES = listOf("jasny" to "Light", "ciemny" to "Dark")

/**
 * Generuje `DesignTokens.kt` w [packageName] z pliku tokenów wizualnych: schematy kolorów Material 3 jasny i
 * ciemny (`kolory`), klasy `StatusColors` (`kolory.status…`) i `MapColors` (`mapa.kolory`) z wartościami dla
 * obu motywów i obiekt stałych w dp dla każdej sekcji wymiarów (`odstepy`, `cele_dotyku`, …). Nazwy przechodzą
 * na camelCase (`droga_kontur` → `drogaKontur`), pole `uzycie` staje się KDoc. Zmiana [tokensFile] unieważnia
 * wynik; brak wymaganej sekcji albo wartości przerywa budowanie.
 */
@CacheableTask
abstract class GenerateDesignTokens : DefaultTask() {
    /** Plik tokenów wizualnych. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val tokensFile: RegularFileProperty

    /** Pakiet generowanego kodu. */
    @get:Input
    abstract val packageName: Property<String>

    /** Katalog wyniku; zadanie czyści go przed zapisem. */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /** Czyta tokeny i zapisuje `DesignTokens.kt`. */
    @TaskAction
    fun generate() {
        val tokens = tokensFile.get().asFile.inputStream().use {
            Load(LoadSettings.builder().build()).loadFromInputStream(it)
        }.asMap()
        val file = FileSpec.builder(packageName.get(), "DesignTokens")
        val (status, scheme) = tokens.sub("kolory").toList().partition { it.first.contains("status", ignoreCase = true) }
        THEMES.forEach { (theme, prefix) ->
            val builder = MemberName(M3, prefix.lowercase() + "ColorScheme")
            file.addProperty(internal("${prefix}ColorScheme", ClassName(M3, "ColorScheme"), "%M%L", builder, colors(scheme.toMap(), theme)))
        }
        palette(file, ClassName(packageName.get(), "StatusColors"), status.toMap())
        palette(file, ClassName(packageName.get(), "MapColors"), tokens.sub("mapa").sub("kolory"))
        tokens.filterValues { section -> section is Map<*, *> && section.values.all { it is Map<*, *> && "dp" in it } }
            .forEach { (name, section) -> dimensions(file, name, section.asMap()) }
        file.build().writeTo(outputDirectory.get().asFile.apply { deleteRecursively() })
    }

    private fun palette(file: FileSpec.Builder, type: ClassName, colors: Map<String, Any?>) {
        val constructor = FunSpec.constructorBuilder().addParameters(colors.keys.map { ParameterSpec(camel(it), COLOR) })
        val properties = colors.map { (key, value) ->
            PropertySpec.builder(camel(key), COLOR).initializer(camel(key)).addKdoc("%L", value.asMap().require("uzycie")).build()
        }
        file.addType(
            TypeSpec.classBuilder(type)
                .addKdoc("Kolory spoza ról Material 3 z pliku tokenów, osobno dla motywu jasnego i ciemnego.")
                .primaryConstructor(constructor.build())
                .addProperties(properties)
                .build(),
        )
        THEMES.forEach { (theme, prefix) ->
            file.addProperty(internal(prefix + type.simpleName, type, "%T%L", type, colors(colors, theme)))
        }
    }

    private fun dimensions(file: FileSpec.Builder, name: String, section: Map<String, Any?>) {
        val type = TypeSpec.objectBuilder(camel(name).replaceFirstChar(Char::uppercase))
            .addKdoc("Wymiary z sekcji `%L` pliku tokenów.", name)
        section.forEach { (key, value) ->
            val entry = value.asMap()
            val usage = entry.require("uzycie").toString()
            type.addProperty(constant(camel(key), ClassName(UNIT, "Dp"), CodeBlock.of("%L.%M", entry.require("dp"), DP), usage))
            entry.filterValues { it is String && it.startsWith("#") }.forEach { (field, hex) ->
                type.addProperty(constant(camel("${key}_$field"), COLOR, color(hex), usage))
            }
        }
        file.addType(type.build())
    }

    private fun internal(name: String, type: TypeName, format: String, vararg args: Any) =
        PropertySpec.builder(name, type, KModifier.INTERNAL).initializer(format, *args).build()

    private fun constant(name: String, type: TypeName, value: CodeBlock, doc: String) =
        PropertySpec.builder(name, type).initializer(value).addKdoc("%L", doc).build()

    private fun colors(colors: Map<String, Any?>, theme: String) =
        colors.map { (key, value) -> CodeBlock.of("%N = %L", camel(key), color(value.asMap().require(theme))) }.args()

    private fun color(hex: Any?) = CodeBlock.of("%T(0xFF%L)", COLOR, hex.toString().removePrefix("#").uppercase())

    private fun List<CodeBlock>.args() = joinToCode(",\n", "(\n⇥", ",\n⇤)")

    private fun camel(name: String) =
        name.split('_').mapIndexed { index, part -> if (index == 0) part else part.replaceFirstChar(Char::uppercase) }.joinToString("")

    private fun Any?.asMap(): Map<String, Any?> =
        (this as? Map<*, *>)?.mapKeys { it.key.toString() } ?: error("Oczekiwano sekcji w pliku tokenów, jest: $this")

    private fun Map<String, Any?>.sub(key: String) = require(key).asMap()

    private fun Map<String, Any?>.require(key: String): Any = this[key] ?: error("Brak klucza `$key` w pliku tokenów")
}
