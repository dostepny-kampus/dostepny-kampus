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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Pola logiczne zgłoszenia z serwera: brak pola to `false`, jak w kontrakcie (`default: false`), więc oba dają ten
 * sam model; `false` nie trafia do wysyłki, `true` wraca jako `true`.
 */
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
        assertAbsentMeansFalse(name = "manualCorrection") { bare(flag = """"manualCorrection":$it,""") }
    }

    @Test
    fun us8_4_removed_absent_false() {
        assertAbsentMeansFalse(name = "barrierRemoved") { bare(flag = """"barrierRemoved":$it,""") }
    }

    @Test
    fun us7_2_mezzanine_absent_false() {
        assertAbsentMeansFalse(name = "mezzanine", scope = { it.getValue("floor").jsonObject }) {
            bare(floor = ""","mezzanine":$it""")
        }
    }

    private fun assertAbsentMeansFalse(
        name: String,
        scope: (JsonObject) -> JsonObject = { it },
        explicit: (Boolean) -> String,
    ) {
        val (absentContent, absentSent) = resend(bare())
        val (falseContent, _) = resend(explicit(false))
        val (_, trueSent) = resend(explicit(true))
        assertEquals(falseContent, absentContent, "Brak pola $name i jawne false dają różne modele")
        assertNull(scope(absentSent)[name], "Pole $name równe false trafiło do wysyłki")
        assertEquals(JsonPrimitive(true), scope(trueSent)[name], "Jawne true w $name zginęło przy wysyłce")
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
