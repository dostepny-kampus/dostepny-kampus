/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import org.gradle.api.provider.ProviderFactory

/**
 * Adres usługi w polu BuildConfig [field]. Debug ma stały adres [debugUrl]. Release bierze właściwość Gradle
 * [property] albo zmienną środowiska [variable]; bez nich kompiluje się z adresem [placeholder], a plik APK
 * i pakiet odrzuca [ReleaseAddressCheck].
 */
enum class ServerAddress(
    val field: String,
    val property: String,
    val variable: String,
    val debugUrl: String,
    val placeholder: String,
) {
    /** Adres API serwera. */
    API(
        field = "API_BASE_URL",
        property = "dk.apiUrl",
        variable = "DK_API_URL",
        debugUrl = "http://10.0.2.2:8080/",
        placeholder = "https://serwer.invalid/",
    ),

    /** Adres realmu usługi logowania. */
    LOGIN_REALM(
        field = "LOGIN_REALM_URL",
        property = "dk.loginRealmUrl",
        variable = "DK_LOGIN_REALM_URL",
        debugUrl = "http://10.0.2.2:8180/realms/dostepny-kampus/",
        placeholder = "https://logowanie.invalid/realms/dostepny-kampus/",
    ),
    ;

    /** Wartość pola BuildConfig dla debug: tekst Javy w cudzysłowach. */
    val debugField: String
        get() = quoted(debugUrl)

    /** Adres release z konfiguracji budowania; `null`, gdy nie ustawiono ani właściwości, ani zmiennej. */
    fun release(providers: ProviderFactory): String? =
        providers.gradleProperty(property).orElse(providers.environmentVariable(variable)).orNull

    /** Wartość pola BuildConfig dla release: adres z konfiguracji albo [placeholder]. */
    fun releaseField(providers: ProviderFactory): String = quoted(release(providers) ?: placeholder)

    /** Opis błędu adresu release [value] albo `null`, gdy adres jest poprawny: `https://` i ukośnik na końcu. */
    fun problem(value: String?): String? =
        when {
            value.isNullOrBlank() -> "brak adresu release $field: ustaw -P$property=… albo zmienną $variable"
            !value.startsWith("https://") -> "adres release $field ($property) musi zaczynać się od https://"
            !value.endsWith("/") -> "adres release $field ($property) musi kończyć się ukośnikiem"
            else -> null
        }

    private fun quoted(url: String): String = "\"$url\""
}
