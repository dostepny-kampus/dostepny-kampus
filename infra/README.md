# Infrastruktura lokalna

`docker-compose.yml` uruchamia bazę serwera, czyli PostgreSQL z PostGIS (oficjalny obraz `postgis/postgis`). Uruchamia też magazyn zdjęć zgłoszeń RustFS, zgodny z S3 (obraz `rustfs/rustfs`, [ADR-0009](../docs/docs/architektura/decyzje/0009-magazyn-zdjec.md)). Usługa logowania i serwer dojdą tu w kolejnych zadaniach.

## Uruchomienie

```sh
cp infra/.env.example infra/.env
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml down -v
```

Baza i magazyn są gotowe, gdy `ps` pokazuje stan `healthy`. Porty są otwarte tylko na `127.0.0.1`. `down -v` usuwa kontenery razem z danymi.

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
