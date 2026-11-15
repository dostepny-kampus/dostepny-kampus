# :feature:capture

## Zadanie

Zgłoszenie bariery: pomiar położenia, formularz z zdjęciami, szybkie zgłoszenie wejścia i lista własnych zgłoszeń.

## Pakiety

- `feature.capture.location` — budynek dla położenia i zgoda na lokalizację.
- `feature.capture.measure` — ekran dokładnego pomiaru położenia z mapą nieba.
- `feature.capture.mine` — lista moich zgłoszeń z kartami i statusami.
- `feature.capture.navigation` — trasy ekranów zgłoszeń.
- `feature.capture.photo` — aparat, plik i podgląd zdjęcia zgłoszenia.
- `feature.capture.quick` — szybkie zgłoszenie wejścia krok po kroku.
- `feature.capture.report` — szkic pełnego zgłoszenia, jego akcje i zapis stanu.
- `feature.capture.report.edit` — edycja wysłanego zgłoszenia.
- `feature.capture.report.form` — formularz pełnego zgłoszenia: pola, wybory i komunikaty.
- `feature.capture.report.removed` — zgłoszenie usunięcia bariery z przyjętego zgłoszenia.

## API

Trasy pomiaru, formularza, poprawki i listy zgłoszeń w pakiecie `navigation`.

## Zależności

- `:core:data`
- `:core:designsystem`
- `:core:geo`
- `:core:location`
- `:core:model`

## Czego moduł nie robi

Nie wysyła zgłoszeń sam; zapis i kolejka wysyłki są w `:core:data`. Nie pokazuje mapy kampusu.

## Kiedy dodawać tutaj

Kod kroków zgłoszenia i listy własnych zgłoszeń. Pobieranie i zapis danych należy do `:core:data`.
