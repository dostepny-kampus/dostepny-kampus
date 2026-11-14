/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

/**
 * Element, który daje rolę bez logowania, na czas przed logowaniem w aplikacji (ADR-0003). Każde użycie wymaga
 * jawnej zgody `@OptIn(TymczasoweRole::class)`, więc lista miejsc do poprawy jest zawsze pełna.
 *
 * TODO(#877): usunąć razem z wprowadzeniem logowania (repozytorium logowania albo AppAuth).
 * Test-strażnik w `app` pada, gdy logowanie już jest, a ta adnotacja nadal istnieje; po jej usunięciu kompilator
 * wskaże każde użycie.
 */
@RequiresOptIn(
    message = "Rola bez logowania jest tymczasowa; użycie wymaga @OptIn(TymczasoweRole::class)",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class TymczasoweRole
