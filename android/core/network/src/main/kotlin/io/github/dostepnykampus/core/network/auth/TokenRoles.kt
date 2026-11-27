/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.auth

import io.github.dostepnykampus.core.network.decodeContract
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

/** Identyfikator konta (`sub`) z tokenu JWT, albo `null`, gdy go nie ma. */
public fun subjectOf(token: String): String? = claimsOf(token)?.textClaim("sub")

private fun JsonObject.textClaim(key: String): String? =
    (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.takeIf { it.isNotBlank() }

private fun claimsOf(token: String): JsonObject? {
    val payload = token.split('.').takeIf { it.size == JWT_PARTS }?.get(1) ?: return null
    return try {
        val text = String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
        Json.decodeContract(JsonObject.serializer(), text)
    } catch (_: IllegalArgumentException) {
        null
    }
}

private const val JWT_PARTS = 3

/**
 * Dane konta z tokenu dostępu.
 *
 * @property realmRoles nazwy ról realmu
 * @property displayName nazwa osoby albo `null`, gdy nie ma jej w tokenie
 */
public data class TokenAccountClaims(val realmRoles: List<String>, val displayName: String?)

/**
 * Odczytuje role realmu (`realm_access.roles`) i nazwę osoby (`name` lub `preferred_username`)
 * z treści tokenu dostępu bez weryfikacji podpisu.
 *
 * Wymagania: BR-1
 *
 * @param token token JWT
 * @return dane konta albo `null`, gdy tokenu nie da się odczytać
 */
public fun accountClaimsOf(token: String): TokenAccountClaims? {
    val claims = claimsOf(token) ?: return null
    return try {
        val access = claims["realm_access"]?.jsonObject
        val roles = access?.get("roles")?.jsonArray
        TokenAccountClaims(
            realmRoles = roles?.map { it.jsonPrimitive.content }.orEmpty(),
            displayName = claims.textClaim("name") ?: claims.textClaim("preferred_username"),
        )
    } catch (_: IllegalArgumentException) {
        null
    }
}
