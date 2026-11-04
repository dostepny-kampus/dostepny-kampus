/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Zdjęcia zgłoszeń: dodanie zdjęcia z listy photoIds (BR-7, BR-8)
 * i odczyt zdjęcia zgłoszenia przyjętego (US2.2).
 * Do sprintu 5 zdjęcia zgłoszenia przyjętego nie zmienia nikt.
 *
 * @since 0.1.0
 */
@RestController
public final class SubmissionPhotosController {

    /**
     * Zdjęcia zgłoszeń.
     */
    private final SubmissionPhotos photos;

    SubmissionPhotosController(final SubmissionPhotos photos) {
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
     * @throws IOException Gdy nie da się odczytać ciała żądania
     */
    @PutMapping(
        path = "/submissions/{submissionId}/photos/{photoId}",
        consumes = MediaType.IMAGE_JPEG_VALUE
    )
    ResponseEntity<Void> putSubmissionPhoto(
        @PathVariable("submissionId") final UUID submission,
        @PathVariable("photoId") final UUID photo,
        final HttpServletRequest request
    ) throws IOException {
        this.photos.put(
            submission, photo, request.getInputStream(), request.getContentLengthLong()
        );
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
        return this.photos.get(submission, photo);
    }

    /**
     * Odpowiedź 413 {@code application/problem+json} dla zdjęcia powyżej limitu NFR-34.
     *
     * @param error Przekroczenie limitu rozmiaru
     * @return Problem ze statusem 413 i opisem limitu
     */
    @ExceptionHandler
    ProblemDetail tooLarge(final PhotoTooLargeException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, error.getMessage());
    }
}
