#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: zamknij-issue.sh <numer PR>. GitHub zamyka issue z „Closes #N” tylko dla PR do gałęzi
# domyślnej, więc po scaleniu PR do gałęzi sprintu skrypt sam czyta opis PR i zamyka znalezione issue.
pr="${1:?Podaj numer PR}"

body="$(gh pr view "$pr" --json body --jq .body)"
# Closes|Fixes|Resolves #N, wielkość liter bez znaczenia; słowo kluczowe nie może być końcem innego słowa.
issues="$(printf '%s\n' "$body" | grep -oiP '(?<!\w)(?:closes|fixes|resolves)\s+#\K\d+' | sort -un || true)"

if [[ -z $issues ]]; then
  echo "Opis PR #${pr} nie zawiera Closes/Fixes/Resolves #N, nic do zamknięcia."
  exit 0
fi

# Próbujemy zamknąć każde issue; błąd jednego nie może zostawić kolejnych otwartych.
failed=''
for issue in $issues; do
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
