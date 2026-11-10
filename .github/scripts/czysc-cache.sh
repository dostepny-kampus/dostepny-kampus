#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Usuwa poprzednie wpisy cache Actions tej gałęzi (GITHUB_REF) dla każdego podanego prefiksu klucza,
# zostawiając najnowszy wpis (sortowanie po created_at malejąco). Limit pamięci cache repozytorium to 10 GB.
# Użycie: czysc-cache.sh <prefiks klucza>...
for prefiks in "$@"; do
  ids="$(gh cache list --ref "$GITHUB_REF" --key "$prefiks" --limit 100 \
    --sort created_at --order desc --json id --jq '.[].id')"
  # Pierwszy wiersz to najnowszy wpis, zostaje.
  while read -r id; do
    [[ -n $id ]] || continue
    gh cache delete "$id"
    echo "Usunięto poprzedni wpis cache ${id} (${prefiks})."
  done < <(printf '%s\n' "$ids" | tail -n +2)
done
