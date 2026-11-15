# :core:database

## Zadanie

Lokalna baza Room: kopie budynków, barier i planów pięter, zgłoszenia własne i oczekujące na wysłanie.

## Pakiety

- `core.database` — baza Room, jej migracje i wiązania Hilt.
- `core.database.cache` — kopia budynków i przyjętych barier z serwera.
- `core.database.inspection` — zmiany znaczników piętra obejrzanego czekające na wysłanie.
- `core.database.room` — kopia planów pięter i ich pomieszczeń.
- `core.database.submission` — własne zgłoszenia oraz zgłoszenia i edycje czekające na wysłanie.

## API

DAO i encje. `DkDatabase`, migracje i wiązania Hilt (`DatabaseModule`) są `internal`. Moduły funkcji nie używają bazy bezpośrednio, tylko przez `:core:data`.

## Zależności

Brak zależności od innych modułów projektu.

## Czego moduł nie robi

Nie zna sieci ani interfejsu i nie decyduje, kiedy dane się odświeża.

## Kiedy dodawać tutaj

Nowa tabela, DAO albo migracja. Logika, co i kiedy zapisać, należy do `:core:data`.
