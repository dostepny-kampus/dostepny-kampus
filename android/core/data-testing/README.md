# :core:data-testing

## Zadanie

Repozytoria `:core:data` dla testów modułów funkcji: odpowiedzi serwera bez typów klienta HTTP.

## API

`ServerReply` (wynik, brak sieci, błąd serwera), `ServerBuildings`, `ServerCard` i fabryki `serverBuildingCatalog`, `serverEntrances`.

## Zależności

- `:core:data`
- `:core:model`
- `:core:network`

## Czego moduł nie robi

Nie trafia do aplikacji; tylko `testImplementation` modułów funkcji. Nie udaje bazy danych.

## Kiedy dodawać tutaj

Fabryka repozytorium, której test modułu funkcji potrzebuje z odpowiedzią serwera. Dublery samych ekranów zostają w testach modułu funkcji.
