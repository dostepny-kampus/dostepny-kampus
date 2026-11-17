#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Usuwa poprzednie wpisy cache Actions tej gałęzi (GITHUB_REF) dla każdego podanego prefiksu klucza,
# zostawiając najnowszy wpis (sortowanie po created_at malejąco). Limit pamięci cache repozytorium to 10 GB.
# Wpis usunięty w międzyczasie przez równoległy przebieg innego commita („Could not find a cache”) nie jest błędem.
# Użycie: czysc-cache.sh <prefiks klucza>...
for prefiks in "$@"; do
  ids="$(gh cache list --ref "$GITHUB_REF" --key "$prefiks" --limit 100 \
    --sort created_at --order desc --json id --jq '.[].id')"
  # Pierwszy wiersz to najnowszy wpis, zostaje.
  while read -r id; do
    [[ -n $id ]] || continue
    if ! wynik="$(gh cache delete "$id" 2>&1)"; then
      if [[ $wynik == *'Could not find a cache'* ]]; then
        echo "Wpis cache ${id} (${prefiks}) usunął już inny przebieg."
        continue
      fi
      printf '%s
' "$wynik" >&2
      exit 1
    fi
    echo "Usunięto poprzedni wpis cache ${id} (${prefiks})."
  done < <(printf '%s\n' "$ids" | tail -n +2)
done
