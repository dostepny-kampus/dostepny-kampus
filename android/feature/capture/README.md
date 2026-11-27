# :feature:capture

## Zadanie

Zgłoszenie bariery: pomiar położenia, formularz z zdjęciami, szybkie zgłoszenie wejścia i lista własnych zgłoszeń.

## Pakiety

- `feature.capture.conflict` — widok i model widoku wyboru jednej z dwóch wersji zgłoszenia po konflikcie wersji.
- `feature.capture.location` — budynek dla położenia i zgoda na lokalizację.
- `feature.capture.measure` — ekran dokładnego pomiaru położenia z mapą nieba i wyborem budynku zgłoszenia.
- `feature.capture.mine` — lista moich zgłoszeń z kartami i statusami.
- `feature.capture.navigation` — trasy ekranów zgłoszeń.
- `feature.capture.photo` — aparat, plik, podgląd zdjęcia zgłoszenia i wspólne ostrzeżenie związane ze zdjęciami.
- `feature.capture.quick` — szybkie zgłoszenie wejścia krok po kroku.
- `feature.capture.report` — ekran pełnego zgłoszenia: szkic, akcje, model widoku formularza i zapis stanu.
- `feature.capture.report.edit` — edycja wysłanego zgłoszenia.
- `feature.capture.report.form` — model formularza pełnego zgłoszenia ze stanem edycji, pola, wybory i komunikaty.
- `feature.capture.report.marking` — zgłoszenie zmiany oznaczenia wejścia.
- `feature.capture.report.removed` — zgłoszenie usunięcia bariery z przyjętego zgłoszenia.
- `feature.capture.sync` — widok i model widoku synchronizacji bez konfliktu: kolejka, postęp i „Synchronizuj teraz”.

## API

Trasy pomiaru, formularza, poprawki, zmiany oznaczenia wejścia, listy zgłoszeń, wyboru wersji i synchronizacji w pakiecie `navigation`.

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
