# :core:network

## Zadanie

Klient HTTP serwera: budynki, zgłoszenia, zmiany znaczników piętra obejrzanego i tokeny dostępu.

## Pakiety

- `core.network` — wspólna wymiana HTTP z wynikiem `ApiResult`, parser kontraktu i wiązania Hilt.
- `core.network.auth` — token dostępu, role realmu i kontrola właściciela przy wysyłce wpisu kolejki.
- `core.network.building` — klient listy budynków, karty budynku, podpowiedzi punktu i ścieżek dostosowanych.
- `core.network.inspection` — klienty znaczników piętra obejrzanego i wysyłki ich zmian.
- `core.network.submission` — klient zgłoszeń i zdjęć.
- `core.network.sync` — klient pobrania zmian z serwera (`GET /sync/pull`), na razie ze znacznikami pięter.

## API

Interfejsy API (`BuildingsApi`, `SubmissionsApi`, `MarkerChangesApi`, `InspectionProgressApi`, `SyncPullApi`), modele JSON i wiązania Hilt (`NetworkModule`). Moduły funkcji nie używają sieci bezpośrednio, tylko przez `:core:data`.

Modele JSON z pakietu `json` generuje przy budowie openapi-generator z `api/openapi.yaml`; zmiana kontraktu
zmienia je bez edycji kodu.
Tekst JSON treści HTTP powstaje i jest czytany tylko w `ContractJson.kt` (`encodeContract`, `decodeContract`);
klienci HTTP pracują na modelach.

## Zależności

- `:core:geo`
- `:core:model`

## Czego moduł nie robi

Nie zapisuje danych lokalnie i nie decyduje o ponowieniach wysyłki.

## Kiedy dodawać tutaj

Nowa operacja z `api/openapi.yaml` albo zmiana nagłówków żądań. Kolejka i synchronizacja należą do `:core:data`.
