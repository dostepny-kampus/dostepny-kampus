#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

base="$1"
head="$2"
found=0

if [[ -z "${FORBIDDEN_PATTERNS:-}" ]]; then
  echo "::error title=Zakazane słowa::Brak sekretu FORBIDDEN_PATTERNS (repozytorium albo Dependabota)."
  exit 2
fi

check() {
  local where="$1" text="$2" matches status=0
  matches="$(printf '%s\n' "$text" | grep -oP -- "$FORBIDDEN_PATTERNS" 2>/dev/null)" || status=$?
  if ((status == 1)); then
    return 0
  fi
  if ((status > 1)); then
    echo "::error title=Zakazane słowa::Niepoprawny wzorzec FORBIDDEN_PATTERNS."
    exit 2
  fi
  echo "::error title=Zakazane słowa::${where}: $(sort -uf <<<"$matches" | paste -sd ',' -)"
  found=1
}

check "gałąź" "${PR_BRANCH:-}"
check "tytuł PR" "${PR_TITLE:-}"
check "opis PR" "${PR_BODY:-}"
commits="$(git rev-list "$base..$head")"
for sha in $commits; do
  check "commit ${sha:0:7}" "$(git log -1 --format=%B "$sha")"
done

# Dodane i zmienione pliki: ścieżka oraz dodane linie (pliki binarne git pomija w diffie).
while IFS= read -r -d '' path; do
  check "nazwa pliku $path" "$path"
  check "plik $path" "$(git diff --unified=0 --no-color "$base...$head" -- "$path" | sed -n '/^+++ /d; s/^+//p')"
done < <(git diff -z --name-only --diff-filter=AMR "$base...$head")

if ((found == 0)); then
  echo "Brak zakazanych słów w gałęzi, tytule, opisie, commitach i dodanych liniach plików PR."
fi
exit "$found"
