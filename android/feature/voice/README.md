# :feature:voice

## Zadanie

Obsługa dialogu głosowego i normalizacja tekstu z rozpoznawania mowy.

## Pakiety

- `feature.voice` — normalizacja tekstu, polecenia wspólne dla kroków, grupa barier z wypowiedzi i kontrakt rozpoznawania mowy.

## API

Funkcje `normalizeSpeechText`, `recognizeSpeechCommand` i `recognizeBarrierGroup`, typ `SpeechCommand`, interfejs `SpeechToText` i jego wynik
`SpeechResult` w pakiecie `feature.voice`.
Atrapa `FakeSpeechToText` jest w testach modułu.

## Zależności

- `:core:model`

## Czego moduł nie robi

Nie zapisuje zgłoszeń ani danych konta i nie wywołuje bezpośrednio sieci ani bazy.

## Kiedy dodawać tutaj

Kod związany z dialogiem głosowym: normalizacja wypowiedzi, rozpoznawanie intencji i kroki dialogu.
