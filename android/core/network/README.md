# :core:network

## Zadanie

Klient HTTP serwera: budynki, zgłoszenia, ich sprawdzanie, zmiany znaczników piętra obejrzanego i tokeny dostępu.

## Pakiety

- `core.network` — wspólna wymiana HTTP z wynikiem `ApiResult`, właścicielem wpisu kolejki i parserem kontraktu.
- `core.network.auth` — token dostępu, role realmu i kontrola właściciela przy wysyłce wpisu kolejki.
- `core.network.building` — klient listy budynków, karty budynku, podpowiedzi punktu i ścieżek dostosowanych.
- `core.network.di` — wiązania Hilt klienta HTTP i interfejsów API.
- `core.network.export` — klient eksportu GeoJSON i CSV dla Koordynatora.
- `core.network.inspection` — klienty znaczników piętra obejrzanego i wysyłki ich zmian.
- `core.network.invitation` — klient zaproszeń Koordynatora.
- `core.network.review` — klient sprawdzania zgłoszeń przez Weryfikatora: kolejka, wzięcie, decyzja, podobne zgłoszenia.
- `core.network.submission` — klient zgłoszeń i zdjęć.
- `core.network.sync` — klient pobrania zmian z serwera (`GET /sync/pull`), na razie ze znacznikami pięter.

## API

Interfejsy API (`BuildingsApi`, `SubmissionsApi`, `MarkerChangesApi`, `InspectionProgressApi`, `SyncPullApi`, `ReviewApi`), modele JSON i wiązania Hilt (`di.NetworkModule`). Moduły funkcji nie używają sieci bezpośrednio, tylko przez `:core:data`.

Modele JSON z pakietu `json` generuje przy budowie openapi-generator z `api/openapi.yaml`; zmiana kontraktu
zmienia je bez edycji kodu.
Tekst JSON treści HTTP powstaje i jest czytany tylko w `ContractJson.kt` (`encodeContract`, `decodeContract`);
klienci HTTP pracują na modelach.

Adres serwera to `BuildConfig.API_BASE_URL`. Debug ma `http://10.0.2.2:8080/`, czyli komputer gospodarza
emulatora. Release bierze adres z właściwości Gradle `dk.apiUrl` albo ze zmiennej środowiska `DK_API_URL`
(np. `./gradlew assembleRelease -Pdk.apiUrl=https://serwer.example/`). Adres musi zaczynać się od `https://`
i kończyć ukośnikiem. Bez takiego adresu wersja release się kompiluje, ale zadanie `checkReleaseAddresses`
zatrzymuje budowę pliku APK i pakietu.

## Zależności

- `:core:geo`
- `:core:model`

## Czego moduł nie robi

Nie zapisuje danych lokalnie i nie decyduje o ponowieniach wysyłki.

## Kiedy dodawać tutaj

Nowa operacja z `api/openapi.yaml` albo zmiana nagłówków żądań. Kolejka i synchronizacja należą do `:core:data`.
