#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: zamknij-nadrzedne.sh <numer właśnie zamkniętego issue>
# Zamyka issue nadrzędne, gdy wszystkie jego sub-issue są zamknięte, i sprawdza kolejne poziomy w górę. Nadrzędnego,
# które kod w bieżącym katalogu nadal wskazuje, nie zamyka: zostawia komentarz z miejscami i kończy.
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

# Wypisuje „plik:wiersz” miejsc kodu bieżącego katalogu (kopia gałęzi domyślnej), które wskazują issue $1:
# znacznik zadania z numerem w kodzie i workflow albo numer w Kotlinie, Javie i scenariuszach (powody rozwiązań
# tymczasowych, wyjątki reguł, komentarze tagów). Takie issue po zamknięciu zaczerwieniłoby kontrolę znaczników.
wskazania() {
  {
    git grep -niE "([t]odo|[f]ixme|[h]ack|@[t]odo)\(#$1\)" -- "${kod[@]}" '*.yml' '*.yaml' || true
    git grep -nE "#$1([^0-9A-Za-z]|$)" -- '*.kt' '*.java' '*.feature' || true
  } | cut -d: -f1,2 | sort -u
}

# shellcheck source=pliki-kodu.sh
source "$(dirname "${BASH_SOURCE[0]}")/pliki-kodu.sh"
mapfile -t kod < <(wzorce_plikow_kodu)

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
  miejsca="$(wskazania "$parent" | paste -sd, - | sed 's/,/, /g')"
  if [[ -n $miejsca ]]; then
    tresc="Wszystkie sub-issue zamknięte (ostatnie: #${child}), ale kod nadal wskazuje to issue, więc zostaje otwarte:"
    gh issue comment "$parent" --body "${tresc} ${miejsca}"
    echo "Nadrzędne #${parent} zostaje: ${miejsca}."
    exit 0
  fi
  gh issue close "$parent" --comment "Wszystkie sub-issue zamknięte, ostatnie: #${child}."
  echo "Zamknięto nadrzędne #${parent}."
  child="$parent"
done
