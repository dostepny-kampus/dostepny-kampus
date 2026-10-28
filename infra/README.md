# Infrastruktura lokalna

`docker-compose.yml` uruchamia bazę serwera: PostgreSQL z PostGIS (oficjalny obraz `postgis/postgis`). Usługa logowania i serwer dojdą tu w kolejnych zadaniach.

## Uruchomienie

```sh
cp infra/.env.example infra/.env
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml down -v
```

Baza jest gotowa, gdy `ps` pokazuje stan `healthy`. Port bazy jest otwarty tylko na `127.0.0.1`. `down -v` usuwa kontener razem z danymi.

## Zmienne

Compose czyta zmienne z `infra/.env`, którego nie ma w repozytorium. Serwer czyta te same zmienne ze środowiska. Schemat bazy zakładają migracje Flyway przy starcie serwera.

| Zmienna | Znaczenie | Domyślnie w serwerze |
| --- | --- | --- |
| `DK_DB_NAME` | nazwa bazy | `dostepny_kampus` |
| `DK_DB_USER` | użytkownik bazy | `dostepny_kampus` |
| `DK_DB_PASSWORD` | hasło użytkownika bazy | brak |
| `DK_DB_PORT` | port bazy na `127.0.0.1` | `5432` |
| `DK_DB_HOST` | host bazy, tylko serwer | `127.0.0.1` |
