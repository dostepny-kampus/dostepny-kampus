/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.auth

import io.github.dostepnykampus.core.network.decodeContract
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

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
    val payload = token.split('.').takeIf { it.size == JWT_PARTS }?.get(1) ?: return null
    return try {
        val text = String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
        val access = Json.decodeContract(JsonObject.serializer(), text)["realm_access"]?.jsonObject
        val roles = access?.get("roles")?.jsonArray
        roles?.map { it.jsonPrimitive.content }.orEmpty()
    } catch (_: IllegalArgumentException) {
        null
    }
}
