/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data

import timber.log.Timber

/** Zbiera linie logu Timber z bloku [capture], żeby test mógł sprawdzić ich treść. */
internal object CapturedLogs {
    /** Linie zapisane w [block] w kolejności zapisu; drzewo znika po bloku. */
    fun capture(block: () -> Unit): List<String> {
        val lines = mutableListOf<String>()
        val tree =
            object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    lines += message
                }
            }
        Timber.plant(tree)
        try {
            block()
        } finally {
            Timber.uproot(tree)
        }
        return lines
    }
}
