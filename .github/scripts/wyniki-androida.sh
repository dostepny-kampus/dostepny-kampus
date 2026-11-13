#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Wymagany check `budowanie`: zielony tylko, gdy testy, APK, emulatory i analiza zależności zakończyły się sukcesem.
# Wejście ze zmiennych środowiskowych: ZMIANY, WYNIKI (testy apk emulator analiza-zaleznosci). Po scaleniu (push) check nie działa:
# commit sprawdziła grupa kolejki, a zadanie po scaleniu tylko przekłada cache.
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
