#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: zamknij-issue.sh <numer PR>. GitHub zamyka issue z „Closes #N” tylko dla PR do gałęzi
# domyślnej, więc po scaleniu PR do gałęzi sprintu skrypt sam czyta opis PR i zamyka znalezione issue.
pr="${1:?Podaj numer PR}"

body="$(gh pr view "$pr" --json body --jq .body)"
# shellcheck source=slowa-zamykajace.sh
source "$(dirname "${BASH_SOURCE[0]}")/slowa-zamykajace.sh"
issues="$(printf '%s\n' "$body" | numery_zamykanych_issue)"

if [[ -z $issues ]]; then
  echo "Opis PR #${pr} nie zawiera słowa zamykającego (np. Closes #N), nic do zamknięcia."
  exit 0
fi

# Próbujemy zamknąć każde issue; błąd jednego nie może zostawić kolejnych otwartych.
failed=''
for issue in $issues; do
  # gh issue view zwraca też dane PR; numer PR nie jest issue i nie jest zamykany.
  if ! url="$(gh issue view "$issue" --json url --jq .url)"; then
    failed="${failed:+$failed }#${issue}"
    continue
  fi
  if [[ $url == */pull/* ]]; then
    echo "::warning title=Zamykanie issue::#${issue}: to PR, nie issue; pominięto."
    continue
  fi
  if gh issue close "$issue" --comment "Zrobione w #${pr}."; then
    echo "Zamknięto #${issue}."
  else
    failed="${failed:+$failed }#${issue}"
  fi
done

if [[ -n $failed ]]; then
  echo "::error title=Zamykanie issue::niezamknięte: ${failed}"
  exit 1
fi
