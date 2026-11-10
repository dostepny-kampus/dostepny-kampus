/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Usługa z jednym miejscem: zadania KSP wszystkich modułów wykonują się po kolei, bo równoległe zadania KSP2 w jednym
 * demonie czasem kończyły się pustym wynikiem (brak plików Hilt, test padał dopiero w czasie działania).
 */
abstract class KspSerialService : BuildService<BuildServiceParameters.None>
