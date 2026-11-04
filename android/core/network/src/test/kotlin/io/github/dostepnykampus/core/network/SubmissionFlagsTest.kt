/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.ID
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.PHOTO
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.POINT
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.SERVER_DATA
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.quick
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Pola logiczne zgłoszenia z serwera: brak pola to nie `false`, a ponowna wysyłka zachowuje tę różnicę. */
class SubmissionFlagsTest {
    private val server = SubmissionServer()

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.stop()
    }

    @Test
    fun us6_3_correction_absent_false() {
        assertAbsentDiffersFromFalse(name = "manualCorrection", explicit = bare(flag = """"manualCorrection":false,"""))
    }

    @Test
    fun us8_4_removed_absent_false() {
        assertAbsentDiffersFromFalse(name = "barrierRemoved", explicit = bare(flag = """"barrierRemoved":false,"""))
    }

    @Test
    fun us7_2_mezzanine_absent_false() {
        assertAbsentDiffersFromFalse(name = "mezzanine", explicit = bare(floor = ""","mezzanine":false""")) {
            it.getValue("floor").jsonObject
        }
    }

    private fun assertAbsentDiffersFromFalse(
        name: String,
        explicit: String,
        scope: (JsonObject) -> JsonObject = { it },
    ) {
        val (absentContent, absentSent) = resend(bare())
        val (falseContent, falseSent) = resend(explicit)
        assertNotEquals(absentContent, falseContent, "Brak pola $name i jawne false dają ten sam model")
        assertNull(scope(absentSent)[name], "Pole $name nieobecne w odpowiedzi trafiło do wysyłki")
        assertEquals(JsonPrimitive(false), scope(falseSent)[name], "Jawne false w $name zginęło przy wysyłce")
    }

    private fun resend(body: String): Pair<SubmissionContent, JsonObject> {
        server.enqueue(code = 200, body = body)
        server.enqueue(code = 200, body = body)
        val read = assertInstanceOf(ApiResult.Success::class.java, server.blocking { create(ID, quick) }).value
        val content = assertInstanceOf(Submission::class.java, read).content
        server.blocking { update(id = ID, version = 1, content = content) }
        server.skipRequest()
        return content to server.sentBody()
    }

    private companion object {
        fun bare(flag: String = "", floor: String = ""): String =
            """{$SERVER_DATA"status":"SENT",$flag"kind":"QUICK",$POINT,"floor":{"number":1$floor},""" +
                """"barrierGroup":"OTHER","photoIds":["$PHOTO"]}"""
    }
}
