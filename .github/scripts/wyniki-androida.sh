#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Wymagany check `budowanie`: zielony tylko, gdy każde zadanie z WYNIKI zakończyło się sukcesem.
# Wejście ze zmiennych środowiskowych: ZMIANY, WYNIKI (w grupie kolejki testy; poza nią apk, emulator, przy zmianie
# aplikacji także testy i analiza-zaleznosci). Po scaleniu (push) check nie działa: zadanie tylko przekłada cache.
echo "zmiany: ${ZMIANY}; testy, apk, emulator, analiza zależności: ${WYNIKI}"
if [[ $ZMIANY != success && $ZMIANY != skipped ]]; then
  echo "::error title=Android::Zadanie zmiany zakończyło się wynikiem ${ZMIANY}."
  exit 1
fi
for wynik in ${WYNIKI}; do
  if [[ $wynik != success ]]; then
    echo "::error title=Android::Co najmniej jedno zadanie Androida nie zakończyło się sukcesem."
    exit 1
  fi
done
