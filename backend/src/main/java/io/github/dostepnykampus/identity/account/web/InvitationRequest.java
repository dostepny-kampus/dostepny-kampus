/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.account.web;

import io.github.dostepnykampus.identity.account.StaffRole;
import io.github.dostepnykampus.shared.error.InvalidInput;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Treść zaproszenia według schematu InvitationRequest.
 *
 * <p>Wymagania: US12.1, BR-2</p>
 *
 * @param email Adres e-mail zapraszanej osoby
 * @param role Nazwa roli pracowniczej
 * @since 0.1.0
 */
record InvitationRequest(@Nullable String email, @Nullable String role) {

    /** Adres z jednym znakiem @ i kropką w domenie, bez spacji. */
    private static final Pattern ADDRESS = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    /** Najdłuższy adres e-mail według RFC 5321. */
    private static final int LONGEST = 254;

    /**
     * Sprawdzony adres; brak adresu albo tekst, który nie jest adresem e-mail, daje
     * {@link InvalidInput}.
     *
     * @return Adres e-mail
     */
    String address() {
        if (this.email == null || this.email.length() > InvitationRequest.LONGEST
            || !InvitationRequest.ADDRESS.matcher(this.email).matches()) {
            throw new InvalidInput("Pole email nie jest adresem e-mail");
        }
        return this.email;
    }

    /**
     * Sprawdzona rola; brak roli albo rola spoza ról pracowniczych daje {@link InvalidInput}.
     *
     * @return Rola pracownicza
     */
    StaffRole staffRole() {
        return Arrays.stream(StaffRole.values())
            .filter(known -> known.name().equals(this.role)).findFirst()
            .orElseThrow(
                () -> new InvalidInput("Pole role przyjmuje COLLECTOR, VERIFIER albo COORDINATOR")
            );
    }
}
