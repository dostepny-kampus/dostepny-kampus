#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: cache-budowania-sprintu.sh <gałąź sprintu, np. sprint/4> <katalog build-cache Gradle>
# Grupa kolejki nie widzi cache zapisanego na gałęzi sprintu, więc udana grupa kolejki wysyła build cache jako artefakt
# `gradle-build-cache`, a ten skrypt pobiera go z najnowszego udanego przebiegu `android` ze zdarzenia merge_group
# na gałęziach kolejki tego sprintu (nigdy z przebiegu pull_request). Grupa, która pominęła budowanie, artefaktu nie ma,
# więc skrypt sprawdza kolejne. Brak artefaktu albo błąd nie psuje budowania: zostaje zimny cache.
# Środowisko: GH_TOKEN, GH_REPO; GITHUB_RUN_ID (pomijany, jeśli to ten sam przebieg); CACHE_WORKFLOW (domyślnie android.yml).
branch="${1:?Podaj gałąź sprintu}"
dest="${2:?Podaj katalog docelowy}"

if ! runs="$(gh run list --workflow "${CACHE_WORKFLOW:-android.yml}" --event merge_group --status success --limit 20 \
  --json databaseId,headBranch \
  --jq ".[] | select(.headBranch | startswith(\"gh-readonly-queue/${branch}/\")) | .databaseId")"; then
  echo "::warning title=Cache budowania::Nie udało się odczytać przebiegów android na ${branch}; budowanie bez cache."
  exit 0
fi
for run in $runs; do
  [[ $run != "${GITHUB_RUN_ID:-}" ]] || continue
  rm -rf "$dest"
  if gh run download "$run" --name gradle-build-cache --dir "$dest" && [[ -n $(ls -A "$dest") ]]; then
    echo "Cache budowania z przebiegu ${run} kolejki ${branch}: $(find "$dest" -type f | wc -l) plików."
    exit 0
  fi
done
echo "::notice title=Cache budowania::Brak artefaktu cache na ${branch}; budowanie bez cache."
