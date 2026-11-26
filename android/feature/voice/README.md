# :feature:voice

## Zadanie

Obsługa dialogu głosowego i normalizacja tekstu z rozpoznawania mowy.

## Pakiety

- `feature.voice` — normalizacja tekstu i kontrakt rozpoznawania mowy na potrzeby dialogu.

## API

Funkcja `normalizeSpeechText`, interfejs `SpeechToText` i jego wynik `SpeechResult` w pakiecie `feature.voice`.
Atrapa `FakeSpeechToText` jest w testach modułu.

## Zależności

Brak zależności od innych modułów projektu.

## Czego moduł nie robi

Nie zapisuje zgłoszeń ani danych konta i nie wywołuje bezpośrednio sieci ani bazy.

## Kiedy dodawać tutaj

Kod związany z dialogiem głosowym: normalizacja wypowiedzi, rozpoznawanie intencji i kroki dialogu.
