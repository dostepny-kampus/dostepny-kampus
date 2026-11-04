/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

/**
 * Zdjęcia zgłoszeń: dodanie zdjęcia z listy photoIds (BR-7, BR-8)
 * i odczyt zdjęcia zgłoszenia przyjętego (US2.2).
 *
 * <p>Do sprintu 5 serwer nie zna autora, więc zdjęcia zgłoszenia przyjętego
 * nie zmienia nikt, a odczyt działa tylko dla zgłoszeń przyjętych.</p>
 *
 * @since 0.1.0
 */
@RestController
public final class SubmissionPhotosController {

    /**
     * Zgłoszenia z listą zdjęć.
     */
    private final SubmissionStore submissions;

    /**
     * Magazyn zdjęć.
     */
    private final PhotoStore photos;

    SubmissionPhotosController(final SubmissionStore submissions, final PhotoStore photos) {
        this.submissions = submissions;
        this.photos = photos;
    }

    /**
     * Zapisuje zdjęcie bez metadanych (NFR-26). Ponowne wysłanie tego samego
     * zdjęcia je zastępuje (BR-9).
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia z listy photoIds
     * @param request Wejściowe żądanie JPEG odczytywane z limitem NFR-34
     * @return Odpowiedź 204 bez treści
     */
    @PutMapping(
        path = "/submissions/{submissionId}/photos/{photoId}",
        consumes = MediaType.IMAGE_JPEG_VALUE
    )
    ResponseEntity<Void> putSubmissionPhoto(
        @PathVariable("submissionId") final UUID submission,
        @PathVariable("photoId") final UUID photo,
        final HttpServletRequest request
    ) {
        final boolean locked = this.submissions.find(submission)
            .filter(item -> SubmissionPhotosController.listed(item, photo))
            .map(item -> item.path("status").stringValue(""))
            .map(Set.of("ACCEPTED", "ARCHIVED")::contains)
            .orElseThrow(SubmissionPhotosController::missing);
        if (locked) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Zdjęcie zgłoszenia przyjętego zmienia tylko Weryfikator"
            );
        }
        final byte[] clean;
        try {
            clean = JpegCleaner.strip(
                new PhotoUpload(request.getInputStream(), request.getContentLengthLong()).read()
            );
        } catch (final PhotoTooLargeException ex) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, ex.getMessage(), ex);
        } catch (final IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uszkodzony plik JPEG", ex);
        }
        this.photos.put(submission, photo, clean);
        return ResponseEntity.noContent().build();
    }

    /**
     * Zwraca zdjęcie zgłoszenia przyjętego (US2.2, BR-6).
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia z listy photoIds
     * @return Plik JPEG bez metadanych
     */
    @GetMapping(
        path = "/submissions/{submissionId}/photos/{photoId}",
        produces = MediaType.IMAGE_JPEG_VALUE
    )
    byte[] getSubmissionPhoto(
        @PathVariable("submissionId") final UUID submission,
        @PathVariable("photoId") final UUID photo
    ) {
        return this.submissions.accepted(submission)
            .filter(item -> SubmissionPhotosController.listed(item, photo))
            .flatMap(item -> this.photos.get(submission, photo))
            .orElseThrow(SubmissionPhotosController::missing);
    }

    private static boolean listed(final JsonNode submission, final UUID photo) {
        return StreamSupport.stream(submission.path("photoIds").spliterator(), false)
            .anyMatch(item -> photo.toString().equalsIgnoreCase(item.stringValue("")));
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
