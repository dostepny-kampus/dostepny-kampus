# Infrastruktura lokalna

`docker-compose.yml` uruchamia bazę serwera, czyli PostgreSQL z PostGIS (oficjalny obraz `postgis/postgis`). Uruchamia też magazyn zdjęć zgłoszeń RustFS, zgodny z S3 (obraz `rustfs/rustfs`, [ADR-0009](../docs/docs/architektura/decyzje/0009-magazyn-zdjec.md)). Uruchamia także usługę logowania Keycloak z realmem deweloperskim ([ADR-0003](../docs/docs/architektura/decyzje/0003-keycloak.md)). Serwer dojdzie tu w kolejnych zadaniach.

## Uruchomienie

```sh
cp infra/.env.example infra/.env
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml down -v
```

Baza, magazyn i usługa logowania są gotowe, gdy `ps` pokazuje stan `healthy`. Porty są otwarte tylko na `127.0.0.1`. `down -v` usuwa kontenery razem z danymi.

## Zmienne

Compose czyta zmienne z `infra/.env`, którego nie ma w repozytorium. Serwer czyta te same zmienne ze środowiska. Schemat bazy zakładają migracje Flyway przy starcie serwera.

| Zmienna | Znaczenie | Domyślnie w serwerze |
| --- | --- | --- |
| `DK_DB_NAME` | nazwa bazy | `dostepny_kampus` |
| `DK_DB_USER` | właściciel bazy; na nim idą migracje | `dostepny_kampus` |
| `DK_DB_PASSWORD` | hasło właściciela bazy | brak |
| `DK_DB_APP_USER` | konto serwera: tylko odczyt i zapis wierszy | `dostepny_kampus_serwer` |
| `DK_DB_APP_PASSWORD` | hasło konta serwera | brak |
| `DK_DB_PORT` | port bazy na `127.0.0.1` | `5432` |
| `DK_DB_HOST` | host bazy, tylko serwer | `127.0.0.1` |
| `DK_PHOTOS_ROOT_ACCESS_KEY` | klucz główny magazynu, tylko compose | brak |
| `DK_PHOTOS_ROOT_SECRET_KEY` | sekret klucza głównego | brak |
| `DK_PHOTOS_ACCESS_KEY` | klucz serwera: tylko kubełek zdjęć | brak |
| `DK_PHOTOS_SECRET_KEY` | sekret klucza serwera | brak |
| `DK_PHOTOS_PORT` | port S3 magazynu na `127.0.0.1` | `9000` |
| `DK_PHOTOS_HOST` | host magazynu, tylko serwer | `127.0.0.1` |
| `DK_PHOTOS_BUCKET` | kubełek ze zdjęciami, tylko serwer | `zdjecia` |
| `DK_AUTH_ADMIN_USER` | administrator usługi logowania, tylko lokalnie | brak |
| `DK_AUTH_ADMIN_PASSWORD` | hasło administratora usługi logowania | brak |
| `DK_AUTH_PORT` | port usługi logowania na `127.0.0.1` | `8180` |
| `DK_AUTH_ISSUER` | adres realmu, którego tokeny przyjmuje serwer | `http://localhost:8180/realms/dostepny-kampus` |
| `DK_PUBLIC_RATE_LIMIT` | żądania bez logowania z jednego adresu na minutę (NFR-34) | `60` |
| `DK_RATE_LIMIT_UNLIMITED` | adresy bez limitu, po przecinku | puste |
| `DK_AUTH_AUDIENCE` | odbiorca w `aud`, bez którego serwer odrzuca token | `dostepny-kampus-api` |

## Konta bazy

Migracje Flyway zakładają tabele na koncie właściciela `DK_DB_USER`. Żądania serwer obsługuje na koncie `DK_DB_APP_USER`, które nie jest superużytkownikiem i nie tworzy tabel; ma tylko odczyt i zapis wierszy, także w tabelach z późniejszych migracji. Konto zakłada skrypt `postgis/konto-serwera.sh` przy pierwszym starcie pustej bazy.

Baza założona przed tą zmianą nie ma konta serwera. Po dopisaniu zmiennych do `infra/.env` zakłada się je jednym poleceniem:

```sh
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml exec postgis bash /docker-entrypoint-initdb.d/konto-serwera.sh
```

## Przechowywanie danych (NFR-29)

Serwer codziennie o 3:30 usuwa zgłoszenia odrzucone ponad 90 dni temu razem z ich zdjęciami w magazynie. Dni liczą się od ostatniego odrzucenia; zgłoszenie poprawione i wysłane ponownie nie jest usuwane.

## Klucze magazynu

Klucz główny RustFS (`DK_PHOTOS_ROOT_ACCESS_KEY`) służy tylko do administracji i serwer go nie dostaje. Usługa `zdjecia-konto` przy każdym `up` uruchamia `rustfs/konto-serwera.sh` i kończy pracę. Skrypt zakłada kubełek zdjęć i klucz serwera `DK_PHOTOS_ACCESS_KEY` z polityką: lista, odczyt, zapis i usuwanie obiektów tylko w tym kubełku.

Plik `infra/.env` sprzed tej zmiany ma w `DK_PHOTOS_ACCESS_KEY` klucz główny. Ten klucz przechodzi do `DK_PHOTOS_ROOT_ACCESS_KEY`, a `DK_PHOTOS_ACCESS_KEY` dostaje nową wartość dla serwera; to samo dotyczy sekretów.

## Import planów pięter PWr

Plany pięter importuje się ręcznie ([ADR-0012](../docs/docs/architektura/decyzje/0012-import-planow-pieter.md)). Polecenie czyta warstwy z `backend/src/main/resources/pwr-plans.yaml` i pobiera piętra i pomieszczenia z usługi ArcGIS PWr. Zapisuje je w bazie z sekcji „Uruchomienie” i kończy się bez serwera HTTP.

```sh
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=import-pwr
./mvnw spring-boot:run -Dspring-boot.run.profiles=import-pwr -Dspring-boot.run.arguments=--budynek=A-1
```

Polecenie uruchamia po kolei kroki importu (`PwrImportStep`, kolejność `@Order`); plany pięter są pierwszym krokiem. Błąd kroku przerywa import przed następnymi krokami. Opcja `--budynek` ogranicza import planów do podanych symboli. Ponowne uruchomienie zastępuje plan budynku bez duplikatów. Dziennik podaje liczbę pięter i pomieszczeń każdego budynku.

## Usługa logowania

Keycloak wczytuje przy starcie realm `dostepny-kampus` z pliku `keycloak/realm.json`. Adres realmu to `http://localhost:8180/realms/dostepny-kampus`, jak w kontrakcie API.

Realm jest deweloperski. Ma cztery role z kontraktu, klienta publicznego `dostepny-kampus-android` bez sekretu i po jednym koncie testowym na rolę:

| Rola | Konto | Hasło testowe |
| --- | --- | --- |
| `VISITOR` | `odwiedzajacy.test` | `haslo-testowe-odwiedzajacy` |
| `COLLECTOR` | `zbierajacy.test` | `haslo-testowe-zbierajacy` |
| `VERIFIER` | `weryfikator.test` | `haslo-testowe-weryfikator` |
| `COORDINATOR` | `koordynator.test` | `haslo-testowe-koordynator` |

Realm pozwala założyć konto samemu (`registrationAllowed`). Nowe konto trafia do grupy domyślnej `odwiedzajacy` i dostaje z niej tylko rolę `VISITOR` (US14.3); role pracownicze nadaje Koordynator.

Hasła są jawne, bo konta istnieją tylko w lokalnym realmie i nie mają danych osób. Na serwerze docelowym tego pliku się nie wczytuje.

Klient ma w tym realmie włączone logowanie bezpośrednie nazwą i hasłem (`directAccessGrantsEnabled`). To wyjątek dla dewelopera: z niego korzysta przełącznik ról w aplikacji debug. Konfiguracja docelowa go nie włącza, a aplikacja loguje się kodem autoryzacji z PKCE (NFR-31).

Token dla konta testowego:

```sh
curl -s http://localhost:8180/realms/dostepny-kampus/protocol/openid-connect/token \
  -d grant_type=password -d client_id=dostepny-kampus-android \
  -d username=zbierajacy.test -d password=haslo-testowe-zbierajacy
```

Rola jest w tokenie w polu `realm_access.roles`.

## Usunięcie danych konta (NFR-29)

Żądanie usunięcia danych osobowych administrator wykonuje ręcznie w ciągu miesiąca. Identyfikator konta (`sub`) odczytuje w konsoli usługi logowania, a potem usuwa tam konto. Zgłoszenia zostają bez autora, a znaczniki pięter konta znikają:

```sh
docker compose -f infra/docker-compose.yml exec -T postgis \
  psql -U "$DK_DB_USER" -d "$DK_DB_NAME" -v sub="<identyfikator konta>" <<'SQL'
UPDATE submissions SET author = NULL WHERE author = :'sub';
DELETE FROM inspection_markers WHERE author = :'sub';
SQL
```
