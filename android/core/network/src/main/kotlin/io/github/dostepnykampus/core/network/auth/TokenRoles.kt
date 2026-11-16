/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.auth

import io.github.dostepnykampus.core.network.decodeContract
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

private const val JWT_PARTS = 3

private val claimsJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class TokenClaims(
    @SerialName("realm_access") val realmAccess: RealmAccess = RealmAccess(),
)

@Serializable
private data class RealmAccess(val roles: List<String> = emptyList())

/**
 * Nazwy ról realmu z tokenu dostępu Usługi logowania (`realm_access.roles`). Podpisu tokenu funkcja nie sprawdza;
 * robi to serwer przy każdym żądaniu.
 *
 * Wymagania: BR-1
 *
 * @param token token JWT
 * @return role realmu, pusta lista dla tokenu bez ról, albo `null`, gdy tokenu nie da się odczytać
 */
public fun realmRolesOf(token: String): List<String>? {
    val payload = token.split('.').takeIf { it.size == JWT_PARTS }?.get(1) ?: return null
    return try {
        val text = String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
        claimsJson.decodeContract(TokenClaims.serializer(), text).realmAccess.roles
    } catch (_: IllegalArgumentException) {
        null
    }
}
