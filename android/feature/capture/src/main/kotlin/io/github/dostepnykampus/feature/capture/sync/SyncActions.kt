/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.sync

/**
 * Czynności C4 bez konfliktu.
 *
 * @property onBack „Wstecz”
 * @property onSyncNow „Synchronizuj teraz”
 * @property onLogin „Zaloguj się” przy wygasłym zalogowaniu: L1
 */
data class SyncActions(val onBack: () -> Unit, val onSyncNow: () -> Unit, val onLogin: () -> Unit)
