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
# Opis PR Dependabota to cudze notatki wydania; dla niego sprawdzamy tytuł i commity, nie opis.
if [[ "${PR_AUTHOR:-}" != 'dependabot[bot]' ]]; then
  check "opis PR" "${PR_BODY:-}"
fi
# Przy squashu PR Dependabota GitHub sam dopisuje wiersz o współautorze-bocie; tylko ten jeden wiersz pomijamy.
# Wzorzec ma klasę znaków, żeby ten plik sam nie zawierał sprawdzanego słowa.
bot_trailer='^Co-[a]uthored-by: dependabot\[bot\] <[0-9]+\+dependabot\[bot\]@users\.noreply\.github\.com>$'
commits="$(git rev-list "$base..$head")"
for sha in $commits; do
  check "commit ${sha:0:7}" "$(git log -1 --format=%B "$sha" | grep -vE -- "$bot_trailer" || true)"
done

# Dodane i zmienione pliki: ścieżka oraz dodane linie. --text czyta też plik z atrybutem -diff.
while IFS= read -r -d '' path; do
  check "nazwa pliku $path" "$path"
  # Plik binarny (ma bajt zerowy), np. zrzut PNG: przypadkowe bajty układały się w zakazane słowo. Sprawdzamy tylko nazwę.
  if (($(git show "$head:$path" | LC_ALL=C tr -dc '\000' | wc -c) > 0)); then
    continue
  fi
  check "plik $path" "$(git diff --text --unified=0 --no-color "$base...$head" -- "$path" | sed -n '/^+++ /d; s/^+//p')"
done < <(git diff -z --name-only --diff-filter=AMR "$base...$head")

if ((found == 0)); then
  echo "Brak zakazanych słów w gałęzi, tytule, opisie, commitach i dodanych liniach plików PR."
fi
exit "$found"
