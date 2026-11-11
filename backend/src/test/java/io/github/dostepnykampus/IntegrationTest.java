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
 * Test na pełnym kontekście aplikacji z bazą PostGIS i magazynem RustFS w kontenerach.
 *
 * <p>Wszystkie testy z tą adnotacją mają ten sam zestaw konfiguracji, więc
 * Spring buduje jeden kontekst i po jednym kontenerze bazy i magazynu na cały przebieg.
 * Zestaw obejmuje serwer na losowym porcie, bazę PostGIS, magazyn zdjęć
 * RustFS oraz punkty HTTP tylko dla testów: {@code FailingController} i
 * {@code StrictJsonHttpTest.Binding}, których trasy są poza kontraktem.
 * Używaj jej w każdym teście, który potrzebuje kontekstu Springa; nie
 * dodawaj własnego {@code @SpringBootTest}, {@code @DirtiesContext} ani
 * {@code @MockitoBean}, bo każdy z nich zakłada osobny kontekst.
 * Pilnuje tego {@code TestSetupArchitectureTest}.</p>
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
