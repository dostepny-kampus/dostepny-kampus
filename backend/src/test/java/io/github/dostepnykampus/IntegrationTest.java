/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Test na pełnym kontekście aplikacji: serwer na losowym porcie, PostGIS i magazyn RustFS
 * w kontenerach oraz punkty HTTP tylko dla testów ({@code FailingController},
 * {@code StrictJsonHttpTest.Binding}). Wszystkie testy z tą adnotacją dzielą jeden kontekst
 * i kontenery; nie dodawaj {@code @SpringBootTest}, {@code @DirtiesContext} ani
 * {@code @MockitoBean}, bo każdy zakłada osobny kontekst.
 *
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(
    {
        PostgisTestConfiguration.class,
        RustfsTestConfiguration.class,
        IdentityTestConfiguration.class,
        MockMvcTestConfiguration.class,
        FailingController.class,
        StrictJsonHttpTest.Binding.class
    }
)
public @interface IntegrationTest {
}
