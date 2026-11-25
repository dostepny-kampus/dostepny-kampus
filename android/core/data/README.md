# :core:data

## Zadanie

Repozytoria: łączą sieć i bazę w dane dla ekranów, prowadzą kolejki wysyłki zgłoszeń i znaczników piętra obejrzanego.
Wysyłka obejmuje wpisy bieżącego konta. Pierwsze zalogowanie po migracji przypisuje starsze wpisy obu kolejek.

## Pakiety

- `core.data.account` — rola zalogowanej osoby, sesja konta, logowanie w przeglądarce (AppAuth) i zalogowanie zapisane w pliku szyfrowanym kluczem z Android Keystore.
- `core.data.barrier` — przyjęte bariery z serwera z kopią w telefonie.
- `core.data.building` — katalog budynków, wejścia i piętra z serwera z kopią w telefonie.
- `core.data.cache` — wspólny zapis JSON kopii danych.
- `core.data.di` — wiązania Hilt modułu.
- `core.data.inspection` — znaczniki piętra obejrzanego czekające w kolejce telefonu.
- `core.data.invitation` — zaproszenie osoby z rolą przez Koordynatora.
- `core.data.map` — zapamiętany wybór między mapą a listą budynków.
- `core.data.network` — stan połączenia z siecią.
- `core.data.path` — ścieżki dostosowane z serwera z kopią w telefonie.
- `core.data.photo` — wykonanie zdjęcia zgłoszenia i jego wynik.
- `core.data.room` — plany pięter budynków z serwera z kopią w telefonie.
- `core.data.server` — wynik żądania do serwera bez typów klienta HTTP.
- `core.data.start` — pierwsze uruchomienie aplikacji.
- `core.data.submission.own` — własne zgłoszenia w telefonie i ich statusy.
- `core.data.submission.pending` — poprawki zgłoszeń czekających w kolejce na wysłanie.
- `core.data.submission.sync` — kolejka zgłoszeń z jej wierszami w bazie, wysyłką w tle i wynikami wysyłki.

## API

Repozytoria i przypadki użycia w podpakietach `account`, `barrier`, `building`, `inspection`, `invitation`, `submission` (`own`, `pending`, `sync`), `map`, `network`, `start`; odpowiedź serwera bez typów klienta HTTP (`ServerReply`, `ServerBuildings`, `ServerCard`) w `server`; wiązania Hilt w `di`. Tekst JSON dokumentów kopii w bazie telefonu powstaje i jest czytany tylko w `cache` (`encodeCache`, `decodeCache`); każdą kopię odpowiedzi serwera trzyma `ServerCopy` pod własnym kluczem tabeli `server_copy`. `SubmissionQueue.save` zwraca `SavedSubmission` z identyfikatorem zgłoszenia, bez encji Room. Tabele bazy są za uchwytami `SubmissionStore` i `ServerCopyStore`, które daje Hilt; `:core:database` jest zależnością `implementation`.

## Zależności

- `:core:database`
- `:core:geo`
- `:core:model`
- `:core:network`

## Czego moduł nie robi

Nie zawiera ekranów ani stanu interfejsu i nie zależy od modułów funkcji.

## Kiedy dodawać tutaj

Źródło danych dla więcej niż jednej funkcji albo dane, które muszą przetrwać bez sieci. Stan jednego ekranu zostaje w jego ViewModel, a typy dziedziny w `:core:model`.
