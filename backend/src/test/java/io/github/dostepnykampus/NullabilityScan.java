/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Odczyt refleksją miejsc użycia {@code Optional} i {@code @Nullable} w typach.
 *
 * @since 0.1.0
 */
final class NullabilityScan {

    static List<String> optionalSites(final Class<?> type) {
        final List<String> found = new ArrayList<>(0);
        Arrays.stream(type.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .filter(field -> NullabilityScan.mentions(field.getGenericType()))
            .forEach(field -> found.add(String.format("%s.%s", type, field.getName())));
        NullabilityScan.executables(type).forEach(
            code -> Arrays.stream(code.getParameters())
                .filter(arg -> NullabilityScan.mentions(arg.getParameterizedType()))
                .forEach(arg -> found.add(String.format("%s.%s", type, code.getName())))
        );
        return found;
    }

    static List<String> misplaced(
        final Class<?> type, final Map<String, Set<String>> listed
    ) {
        final Set<String> kept = Optional.of(type).filter(Class::isRecord)
            .map(Class::getSimpleName).map(listed::get).orElse(Set.of());
        final RecordComponent[] parts = Optional
            .ofNullable(type.getRecordComponents()).orElse(new RecordComponent[0]);
        final List<String> found = new ArrayList<>(0);
        Arrays.stream(type.getDeclaredFields())
            .filter(field -> NullabilityScan.nullable(field.getAnnotatedType()))
            .filter(field -> !kept.contains(field.getName()))
            .forEach(field -> found.add(String.format("%s.%s", type, field.getName())));
        Arrays.stream(parts)
            .filter(part -> NullabilityScan.nullable(part.getAnnotatedType()))
            .filter(part -> !kept.contains(part.getName()))
            .forEach(part -> found.add(String.format("%s.%s", type, part.getName())));
        NullabilityScan.executables(type).forEach(
            code -> {
                if (NullabilityScan.nullable(code.getAnnotatedReturnType())
                    && !(code.getParameterCount() == 0 && kept.contains(code.getName()))) {
                    found.add(String.format("%s.%s", type, code.getName()));
                }
                final AnnotatedType[] args = code.getAnnotatedParameterTypes();
                final boolean helper = Modifier.isPrivate(code.getModifiers())
                    && listed.containsKey(type.getSimpleName());
                final boolean canonical = type.getName().equals(code.getName())
                    && args.length == parts.length;
                for (int idx = 0; idx < args.length; ++idx) {
                    if (NullabilityScan.nullable(args[idx]) && !helper
                        && !(canonical && kept.contains(parts[idx].getName()))) {
                        found.add(String.format("%s.%s", type, code.getName()));
                    }
                }
            }
        );
        return found;
    }

    private static Stream<Executable> executables(final Class<?> type) {
        return Stream.<Executable>concat(
            Arrays.stream(type.getDeclaredConstructors()),
            Arrays.stream(type.getDeclaredMethods())
        ).filter(code -> !code.isSynthetic());
    }

    private static boolean mentions(final Type type) {
        final boolean found;
        if (type instanceof ParameterizedType generic) {
            found = NullabilityScan.mentions(generic.getRawType())
                || Arrays.stream(generic.getActualTypeArguments())
                .anyMatch(NullabilityScan::mentions);
        } else if (type instanceof WildcardType wildcard) {
            found = Stream.concat(
                Arrays.stream(wildcard.getUpperBounds()),
                Arrays.stream(wildcard.getLowerBounds())
            ).anyMatch(NullabilityScan::mentions);
        } else {
            found = type == Optional.class;
        }
        return found;
    }

    private static boolean nullable(final AnnotatedType type) {
        final Stream<AnnotatedType> inner;
        if (type instanceof AnnotatedParameterizedType generic) {
            inner = Arrays.stream(generic.getAnnotatedActualTypeArguments());
        } else if (type instanceof AnnotatedArrayType array) {
            inner = Stream.of(array.getAnnotatedGenericComponentType());
        } else {
            inner = Stream.empty();
        }
        return type.isAnnotationPresent(Nullable.class)
            || inner.anyMatch(NullabilityScan::nullable);
    }
}
