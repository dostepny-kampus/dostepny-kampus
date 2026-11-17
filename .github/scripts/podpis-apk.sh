#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
# Sprawdza, że każdy apk ($2…) podpisał certyfikat o skrócie SHA-256 $1 (64 znaki szesnastkowe), według
# `apksigner verify --print-certs` z $APKSIGNER. Skrót czyta z wiersza pierwszego podpisującego niezależnie od
# przedrostka wiersza, wielkości liter i dwukropków. Kod 0 = wszystkie apk z tym kluczem; przy błędzie wypisuje
# pełny wynik apksigner.
set -uo pipefail

klucz="${1:-}"
shift || true
if [[ ! $klucz =~ ^[0-9a-f]{64}$ ]]; then
  echo "::error title=Podpis apk-debug::Brak skrótu SHA-256 klucza debug z repozytorium ('${klucz}')."
  exit 1
fi
if (($# == 0)); then
  echo "::error title=Podpis apk-debug::Brak plików apk do sprawdzenia."
  exit 1
fi

for apk in "$@"; do
  if ! wynik="$("$APKSIGNER" verify --print-certs "$apk" 2>&1)"; then
    echo "::error title=Podpis apk-debug::${apk} nie przechodzi weryfikacji apksigner."
    printf '%s\n' "$wynik"
    exit 1
  fi
  podpis="$(printf '%s\n' "$wynik" | tr -d '\r' |
    awk 'tolower($0) ~ /^signer.*certificate sha-256 digest:/ { sub(/.*[Dd]igest:[[:space:]]*/, ""); print; exit }' |
    tr -d ': ' | tr 'A-F' 'a-f')"
  if [[ $podpis != "$klucz" ]]; then
    echo "::error title=Podpis apk-debug::${apk} ma certyfikat '${podpis}', a klucz debug z repozytorium '${klucz}'."
    printf '%s\n' "$wynik"
    exit 1
  fi
done
echo "Podpis apk-debug: ${klucz}"
