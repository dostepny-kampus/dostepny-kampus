#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: cache-budowania-sprintu.sh <gałąź sprintu, np. sprint/4> <katalog build-cache Gradle>
# Grupa kolejki nie widzi cache zapisanego na gałęzi sprintu, więc push do sprintu wysyła build cache jako artefakt
# `gradle-build-cache`, a ten skrypt pobiera go z najnowszego udanego przebiegu `android` ze zdarzenia push na tej gałęzi
# (nigdy z przebiegu pull_request). Brak artefaktu albo błąd nie psuje budowania: zostaje zimny cache.
# Środowisko: GH_TOKEN, GH_REPO; GITHUB_RUN_ID (pomijany, jeśli to ten sam przebieg); CACHE_WORKFLOW (domyślnie android.yml).
branch="${1:?Podaj gałąź sprintu}"
dest="${2:?Podaj katalog docelowy}"

if ! runs="$(gh run list --workflow "${CACHE_WORKFLOW:-android.yml}" --branch "$branch" --event push --status success --limit 5 \
  --json databaseId --jq '.[].databaseId')"; then
  echo "::warning title=Cache budowania::Nie udało się odczytać przebiegów android na ${branch}; budowanie bez cache."
  exit 0
fi
for run in $runs; do
  [[ $run != "${GITHUB_RUN_ID:-}" ]] || continue
  rm -rf "$dest"
  if gh run download "$run" --name gradle-build-cache --dir "$dest" && [[ -n $(ls -A "$dest") ]]; then
    echo "Cache budowania z przebiegu ${run} na ${branch}: $(find "$dest" -type f | wc -l) plików."
    exit 0
  fi
done
echo "::notice title=Cache budowania::Brak artefaktu cache na ${branch}; budowanie bez cache."
