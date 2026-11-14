# :core:data

## Zadanie

Repozytoria: łączą sieć i bazę w dane dla ekranów, prowadzą kolejkę wysyłki zgłoszeń.

## API

Repozytoria i przypadki użycia w podpakietach `account`, `barriers`, `buildings`, `submissions`, `map`, `network`, `start`; odpowiedź serwera bez typów klienta HTTP (`ServerReply`, `ServerBuildings`, `ServerCard`) w `server`; wiązania Hilt w `di`. `SubmissionQueue.save` zwraca `SavedSubmission` z identyfikatorem zgłoszenia, bez encji Room. Tabele bazy są za uchwytami `SubmissionStore` i `BuildingListStore`, które daje Hilt; `:core:database` jest zależnością `implementation`.

## Zależności

- `:core:database`
- `:core:geo`
- `:core:model`
- `:core:network`

## Czego moduł nie robi

Nie zawiera ekranów ani stanu interfejsu i nie zależy od modułów funkcji.

## Kiedy dodawać tutaj

Źródło danych dla więcej niż jednej funkcji albo dane, które muszą przetrwać bez sieci. Stan jednego ekranu zostaje w jego ViewModel, a typy dziedziny w `:core:model`.
