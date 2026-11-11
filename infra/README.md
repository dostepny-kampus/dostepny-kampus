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
| `DK_DB_USER` | użytkownik bazy | `dostepny_kampus` |
| `DK_DB_PASSWORD` | hasło użytkownika bazy | brak |
| `DK_DB_PORT` | port bazy na `127.0.0.1` | `5432` |
| `DK_DB_HOST` | host bazy, tylko serwer | `127.0.0.1` |
| `DK_PHOTOS_ACCESS_KEY` | klucz dostępu do magazynu zdjęć | brak |
| `DK_PHOTOS_SECRET_KEY` | sekret magazynu zdjęć | brak |
| `DK_PHOTOS_PORT` | port S3 magazynu na `127.0.0.1` | `9000` |
| `DK_PHOTOS_HOST` | host magazynu, tylko serwer | `127.0.0.1` |
| `DK_PHOTOS_BUCKET` | kubełek ze zdjęciami, tylko serwer | `zdjecia` |
| `DK_AUTH_ADMIN_USER` | administrator usługi logowania, tylko lokalnie | brak |
| `DK_AUTH_ADMIN_PASSWORD` | hasło administratora usługi logowania | brak |
| `DK_AUTH_PORT` | port usługi logowania na `127.0.0.1` | `8180` |

## Usługa logowania

Keycloak wczytuje przy starcie realm `dostepny-kampus` z pliku `keycloak/realm.json`. Adres realmu to `http://localhost:8180/realms/dostepny-kampus`, jak w kontrakcie API.

Realm jest deweloperski. Ma cztery role z kontraktu, klienta publicznego `dostepny-kampus-android` bez sekretu i po jednym koncie testowym na rolę:

| Rola | Konto | Hasło testowe |
| --- | --- | --- |
| `VISITOR` | `odwiedzajacy.test` | `haslo-testowe-odwiedzajacy` |
| `COLLECTOR` | `zbierajacy.test` | `haslo-testowe-zbierajacy` |
| `VERIFIER` | `weryfikator.test` | `haslo-testowe-weryfikator` |
| `COORDINATOR` | `koordynator.test` | `haslo-testowe-koordynator` |

Hasła są jawne, bo konta istnieją tylko w lokalnym realmie i nie mają danych osób. Na serwerze docelowym tego pliku się nie wczytuje.

Klient ma w tym realmie włączone logowanie bezpośrednie nazwą i hasłem (`directAccessGrantsEnabled`). To wyjątek dla dewelopera: z niego korzysta przełącznik ról w aplikacji debug. Konfiguracja docelowa go nie włącza, a aplikacja loguje się kodem autoryzacji z PKCE (NFR-31).

Token dla konta testowego:

```sh
curl -s http://localhost:8180/realms/dostepny-kampus/protocol/openid-connect/token \
  -d grant_type=password -d client_id=dostepny-kampus-android \
  -d username=zbierajacy.test -d password=haslo-testowe-zbierajacy
```

Rola jest w tokenie w polu `realm_access.roles`.
