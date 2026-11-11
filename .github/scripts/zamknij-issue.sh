#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: zamknij-issue.sh <numer PR>. GitHub zamyka issue z „Closes #N” tylko dla PR do gałęzi
# domyślnej, więc po scaleniu PR do gałęzi sprintu skrypt sam czyta opis PR i zamyka znalezione issue.
# Po zamknięciu issue zamyka też jego issue nadrzędne, gdy wszystkie sub-issue nadrzędnego są zamknięte,
# i sprawdza kolejne poziomy w górę. Repozytorium bierze z GH_REPO (właściciel/nazwa).
pr="${1:?Podaj numer PR}"

body="$(gh pr view "$pr" --json body --jq .body)"
# shellcheck source=slowa-zamykajace.sh
source "$(dirname "${BASH_SOURCE[0]}")/slowa-zamykajace.sh"
issues="$(printf '%s\n' "$body" | numery_zamykanych_issue)"

if [[ -z $issues ]]; then
  echo "Opis PR #${pr} nie zawiera słowa zamykającego (np. Closes #N), nic do zamknięcia."
  exit 0
fi

# Wypisuje „numer stan wszystkie zamknięte” issue nadrzędnego albo nic, gdy issue go nie ma.
nadrzedne() {
  # shellcheck disable=SC2016
  gh api graphql -F owner="${GH_REPO%/*}" -F name="${GH_REPO#*/}" -F number="$1" -f query='
    query($owner: String!, $name: String!, $number: Int!) {
      repository(owner: $owner, name: $name) { issue(number: $number) {
        parent { number state subIssuesSummary { total completed } } } } }'     --jq '.data.repository.issue.parent | select(. != null)
      | "\(.number) \(.state) \(.subIssuesSummary.total) \(.subIssuesSummary.completed)"'
}

# Zamyka kolejne issue nadrzędne nad issue $1, dopóki każde ma wszystkie sub-issue zamknięte.
zamknij_nadrzedne() {
  local child="$1" info parent state total completed
  while info="$(nadrzedne "$child")" || return 1; do
    [[ -n $info ]] || return 0
    read -r parent state total completed <<<"$info"
    [[ $state == OPEN && $total == "$completed" ]] || return 0
    gh issue close "$parent" --comment "Wszystkie sub-issue zamknięte, ostatnie: #${child}." || return 1
    echo "Zamknięto nadrzędne #${parent}."
    child="$parent"
  done
}

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
    zamknij_nadrzedne "$issue" || failed="${failed:+$failed }nadrzędne-nad-#${issue}"
  else
    failed="${failed:+$failed }#${issue}"
  fi
done

if [[ -n $failed ]]; then
  echo "::error title=Zamykanie issue::niezamknięte: ${failed}"
  exit 1
fi
