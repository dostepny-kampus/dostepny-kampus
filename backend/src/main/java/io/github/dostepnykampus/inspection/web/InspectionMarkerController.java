/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.web;

import io.github.dostepnykampus.inspection.application.MarkerService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cofnięcie pojedynczego znacznika piętra obejrzanego: Koordynator cofa znacznik dowolnego
 * Zbierającego, a Zbierający wyłącznie własny.
 *
 * <p>Wymagania: US11.3, US15.2, BR-20</p>
 *
 * @since 0.1.0
 */
@RestController
public final class InspectionMarkerController {

    /**
     * Operacje na znacznikach piętra.
     */
    private final MarkerService service;

    InspectionMarkerController(final MarkerService service) {
        this.service = service;
    }

    /**
     * Cofa znacznik o podanym identyfikatorze; 204 przy powodzeniu, 403 przy próbie cofnięcia
     * cudzego znacznika przez Zbierającego, 404 przy braku znacznika.
     *
     * <p>Wymagania: US11.3, US15.2, BR-20</p>
     *
     * @param id Identyfikator cofanego znacznika
     * @param account Zalogowane konto z tokenu
     * @param request Bieżące żądanie HTTP z rolami konta
     */
    @DeleteMapping("/inspection-markers/{markerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(
        @PathVariable("markerId") final UUID id, final Principal account,
        final HttpServletRequest request
    ) {
        this.service.revokeOne(
            id, account.getName(), request.isUserInRole("COORDINATOR")
        );
    }
}
