/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationContext;

/**
 * Beany kontekstu testu, których klasy leżą w podanych modułach aplikacji.
 *
 * @since 0.1.0
 */
public final class ModuleBeans {

    /**
     * Kontekst testu.
     */
    private final ApplicationContext context;

    /**
     * Tworzy widok beanów kontekstu.
     *
     * @param context Kontekst testu
     */
    public ModuleBeans(final ApplicationContext context) {
        this.context = context;
    }

    /**
     * Nazwy klas beanów z podanych modułów.
     *
     * @param modules Nazwy modułów, np. {@code barrier}
     * @return Nazwy klas beanów z tych modułów; pusta lista, gdy kontekst ich nie ma
     */
    public List<String> in(final String... modules) {
        return Arrays.stream(this.context.getBeanDefinitionNames())
            .map(this.context::getType)
            .filter(Objects::nonNull)
            .map(Class::getName)
            .filter(name -> ModuleBeans.inside(name, modules))
            .toList();
    }

    private static boolean inside(final String name, final String... modules) {
        return Arrays.stream(modules).anyMatch(
            module -> name.startsWith(String.format("io.github.dostepnykampus.%s.", module))
        );
    }
}
