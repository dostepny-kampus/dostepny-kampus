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
public fun subjectOf(token: String): String? =
    (claimsOf(token)?.get("sub") as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.takeIf { it.isNotBlank() }

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
 * Nazwy ról realmu z tokenu dostępu Usługi logowania (`realm_access.roles`). Podpisu tokenu funkcja nie sprawdza;
 * robi to serwer przy każdym żądaniu.
 *
 * Wymagania: BR-1
 *
 * @param token token JWT
 * @return role realmu, pusta lista dla tokenu bez ról, albo `null`, gdy tokenu nie da się odczytać
 */
public fun realmRolesOf(token: String): List<String>? {
    val claims = claimsOf(token) ?: return null
    return try {
        val access = claims["realm_access"]?.jsonObject
        val roles = access?.get("roles")?.jsonArray
        roles?.map { it.jsonPrimitive.content }.orEmpty()
    } catch (_: IllegalArgumentException) {
        null
    }
}
