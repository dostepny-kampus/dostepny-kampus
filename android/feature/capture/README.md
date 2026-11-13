# feature:capture

## Zadanie

Obsługuje zgłaszanie barier przez osobę korzystającą z aplikacji.

## API

Ekrany zgłoszenia szybkiego, formularza i własnych zgłoszeń oraz ich trasy nawigacji.
Zapis treści i zdjęć odbywa się przez `core:data.reports.ReportSaver`.

## Od kogo zależy

Kod produkcyjny: `:core:model`, `:core:location`, `:core:data`, `:core:geo`, `:core:designsystem`.
Testy deklarują także `:core:database`. Biblioteki są w `build.gradle.kts`.

## Czego nie robi

Kod produkcyjny nie wykonuje żądań HTTP ani zapytań do bazy. Nie definiuje wspólnego zapisu zgłoszenia.

## Kiedy dodawać kod

Tutaj trafia obsługa ekranów zgłaszania i ich stanu. Wspólne reguły zapisu należą do `core:data`.
Osobną ścieżkę użytkownika z własnymi danymi i małym API można wydzielić do nowego modułu.
