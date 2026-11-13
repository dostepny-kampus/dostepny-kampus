# core:data

## Zadanie

Udostępnia dane aplikacji przez repozytoria i kolejkę wysyłki.

## API

`SubmissionQueue.save` zwraca `SavedSubmission` z identyfikatorem zgłoszenia, bez encji Room.
Repozytoria udostępniają odczyty katalogu, zgłoszeń oraz stanu sieci.

## Od kogo zależy

Moduły Gradle: `:core:model`, `:core:database`, `:core:network`, `:core:geo`.
Pełna lista bibliotek jest w `build.gradle.kts`.

## Czego nie robi

Nie renderuje ekranów ani nie zależy od modułów funkcji.

## Kiedy dodawać kod

Tutaj trafia wspólny dostęp do danych aplikacji. Ekrany należą do modułów funkcji, a typy dziedziny do `core:model`.
Nowy moduł wymaga własnego zadania, danych albo ścieżki użytkownika oraz małego API.
