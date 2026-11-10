#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Wymagany check `budowanie`: zielony tylko, gdy testy, APK i emulatory zakończyły się sukcesem.
# Wejście ze zmiennych środowiskowych: ZMIANY, WYNIKI (testy apk emulator), ZDARZENIE, EMULATOR.
echo "zmiany: ${ZMIANY}; testy, apk, emulator: ${WYNIKI}"
if [[ $ZMIANY != success && $ZMIANY != skipped ]]; then
  echo "::error title=Android::Zadanie zmiany zakończyło się wynikiem ${ZMIANY}."
  exit 1
fi
if [[ $ZDARZENIE == push && $EMULATOR == skipped ]]; then
  # Na push emulator jest pominięty celowo (przeszedł w merge_group); testy i apk dalej muszą się udać, bo zapisują cache.
  WYNIKI="${WYNIKI% skipped}"
fi
if [[ $ZDARZENIE == push && " ${WYNIKI} " == *" cancelled "* && " ${WYNIKI} " != *" failure "* ]]; then
  # Push do sprintu anuluje trwający przebieg (cancel-in-progress), bo cache zapisze najnowszy push; to nie porażka kodu.
  # Na pull_request i merge_group anulowanie dalej jest czerwone, żeby niesprawdzony kod nie przeszedł.
  echo "::notice title=Android::Anulowano — nowszy push zapisze cache."
  exit 0
fi
for wynik in ${WYNIKI}; do
  if [[ $wynik != success ]]; then
    echo "::error title=Android::Co najmniej jedno zadanie Androida nie zakończyło się sukcesem."
    exit 1
  fi
done
