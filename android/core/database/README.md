# :core:database

## Zadanie

Lokalna baza Room: kopie budynków i barier, zgłoszenia własne i oczekujące na wysłanie.

## API

`DkDatabase`, DAO i encje oraz wiązania Hilt (`DatabaseModule`). Moduły funkcji nie używają bazy bezpośrednio, tylko przez `:core:data`.

## Zależności

Brak zależności od innych modułów projektu.

## Czego moduł nie robi

Nie zna sieci ani interfejsu i nie decyduje, kiedy dane się odświeża.

## Kiedy dodawać tutaj

Nowa tabela, DAO albo migracja. Logika, co i kiedy zapisać, należy do `:core:data`.
