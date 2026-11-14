# :core:network

## Zadanie

Klient HTTP serwera: budynki, zgłoszenia i tokeny dostępu.

## API

Interfejsy API (`BuildingsApi`, `SubmissionsApi`), modele JSON i wiązania Hilt (`NetworkModule`). Moduły funkcji nie używają sieci bezpośrednio, tylko przez `:core:data`.

Modele JSON z pakietu `json` generuje przy budowie openapi-generator z `api/openapi.yaml`; zmiana kontraktu
zmienia je bez edycji kodu.

## Zależności

- `:core:geo`
- `:core:model`

## Czego moduł nie robi

Nie zapisuje danych lokalnie i nie decyduje o ponowieniach wysyłki.

## Kiedy dodawać tutaj

Nowa operacja z `api/openapi.yaml` albo zmiana nagłówków żądań. Kolejka i synchronizacja należą do `:core:data`.
