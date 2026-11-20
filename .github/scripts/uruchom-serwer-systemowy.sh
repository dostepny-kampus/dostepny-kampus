#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Uruchamiany z katalogu repozytorium po pakuj-serwer-systemowy.sh. Startuje bazę, magazyn zdjęć i logowanie
# z --wait, jednorazowo zakłada konto serwera w magazynie (--wait padłby na zakończonej usłudze zdjecia-konto),
# potem serwer z backend/target, czeka na /buildings i dopisuje w bazie budynek testu systemowego (TS-1),
# którego nie ma w pliku konturów w aplikacji. Dziennik serwera: $RUNNER_TEMP/serwer.log.
# CZAS_STARTU: sekundy czekania na serwer (domyślnie 180).
compose=(docker compose -f infra/docker-compose.yml)
cp infra/.env.example infra/.env
"${compose[@]}" up -d --wait postgis zdjecia logowanie
"${compose[@]}" run --rm zdjecia-konto
set -a
# shellcheck source=/dev/null
. infra/.env
set +a
jar="$(find backend/target -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n 1)"
nohup java -jar "$jar" > "${RUNNER_TEMP}/serwer.log" 2>&1 &
if ! timeout "${CZAS_STARTU:-180}" bash -c 'until curl -fs -o /dev/null http://127.0.0.1:8080/buildings; do sleep 3; done'; then
  echo "::error title=Test systemowy::Serwer nie odpowiedział na /buildings w ${CZAS_STARTU:-180} s."
  cat "${RUNNER_TEMP}/serwer.log"
  exit 1
fi
"${compose[@]}" exec -T postgis psql -v ON_ERROR_STOP=1 -U "$DK_DB_USER" -d "$DK_DB_NAME" -c \
  "INSERT INTO buildings (symbol, name, detail_level, outline, source) VALUES ('TS-1', 'Budynek testu systemowego', 'BASIC', ST_GeomFromText('MULTIPOLYGON(((17.0615 51.1081, 17.0616 51.1081, 17.0616 51.1082, 17.0615 51.1082, 17.0615 51.1081)))', 4326), '© OpenStreetMap contributors') ON CONFLICT (symbol) DO NOTHING;"
