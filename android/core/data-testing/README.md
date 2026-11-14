# :core:data-testing

## Zadanie

Repozytoria `:core:data` dla testów modułów funkcji: odpowiedzi serwera bez typów klienta HTTP.

## API

Fabryki `serverBuildingCatalog` i `serverEntrances`, które przyjmują `ServerReply` z pakietu `core.data.server`.

## Zależności

- `:core:data`
- `:core:database`
- `:core:model`
- `:core:network`

## Czego moduł nie robi

Nie trafia do aplikacji; tylko `testImplementation` modułów funkcji. Nie udaje bazy danych.

## Kiedy dodawać tutaj

Fabryka repozytorium, której test modułu funkcji potrzebuje z odpowiedzią serwera. Dublery samych ekranów zostają w testach modułu funkcji.
