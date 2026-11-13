#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: zamknij-nadrzedne.sh <numer właśnie zamkniętego issue>
# Zamyka issue nadrzędne, gdy wszystkie jego sub-issue są zamknięte, i sprawdza kolejne poziomy w górę.
# Stan liczy z listy sub-issue, a issue podane w argumencie traktuje jako zamknięte niezależnie od odpowiedzi API:
# zaraz po zamknięciu API potrafi jeszcze zwracać je jako otwarte. Repozytorium bierze z GH_REPO (właściciel/nazwa).
child="${1:?Podaj numer zamkniętego issue}"

# Wypisuje „numer stan” issue nadrzędnego, a w kolejnych wierszach „numer stan” każdego jego sub-issue;
# nic, gdy issue nie ma nadrzędnego. Jedno issue ma najwyżej 100 sub-issue, więc jedna strona wystarcza.
nadrzedne() {
  # shellcheck disable=SC2016
  gh api graphql -F owner="${GH_REPO%/*}" -F name="${GH_REPO#*/}" -F number="$1" -f query='
    query($owner: String!, $name: String!, $number: Int!) {
      repository(owner: $owner, name: $name) { issue(number: $number) {
        parent { number state subIssues(first: 100) { nodes { number state } } } } } }' \
    --jq '.data.repository.issue.parent | select(. != null)
      | "\(.number) \(.state)", (.subIssues.nodes[] | "\(.number) \(.state)")'
}

while true; do
  info="$(nadrzedne "$child")"
  [[ -n $info ]] || exit 0
  read -r parent state <<<"$(head -n 1 <<<"$info")"
  [[ $state == OPEN ]] || exit 0
  open=0
  while read -r number substate; do
    if [[ $substate == OPEN && $number != "$child" ]]; then open=$((open + 1)); fi
  done < <(tail -n +2 <<<"$info")
  ((open == 0)) || exit 0
  gh issue close "$parent" --comment "Wszystkie sub-issue zamknięte, ostatnie: #${child}."
  echo "Zamknięto nadrzędne #${parent}."
  child="$parent"
done
