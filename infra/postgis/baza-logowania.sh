#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
# Osobna baza usługi logowania (DK_AUTH_DB_NAME) z właścicielem bazy serwera; używa jej tylko nakładka
# produkcyjna. Skrypt działa przy pierwszym starcie pustej bazy; na istniejącej bazie uruchamia się go ręcznie
# (infra/README.md).
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    -v auth="$DK_AUTH_DB_NAME" -v owner="$POSTGRES_USER" <<'SQL'
SELECT format('CREATE DATABASE %I OWNER %I', :'auth', :'owner')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'auth') \gexec
SQL
