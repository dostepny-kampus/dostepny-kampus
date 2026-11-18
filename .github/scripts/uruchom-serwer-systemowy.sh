#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Uruchamiany z katalogu repozytorium po pakuj-serwer-systemowy.sh. Startuje bazę, magazyn zdjęć i logowanie
# z --wait, jednorazowo zakłada konto serwera w magazynie (--wait padłby na zakończonej usłudze zdjecia-konto),
# potem serwer z backend/target i czeka na /buildings. Dziennik serwera: $RUNNER_TEMP/serwer.log.
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
