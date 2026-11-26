/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import java.io.IOException

/** Właściciel wpisu kolejki, z którego powstało żądanie. */
internal data class QueueOwner(val subject: String)

/** Lokalna odmowa przed wysłaniem wpisu kolejki. */
internal class QueueOwnerMismatch : IOException("Brak tokenu właściciela wpisu kolejki")
