#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: czekaj-na-obszar.sh <numer PR>. Czeka, aż PR dostanie etykietę obszaru z .github/labeler.yml, najwyżej
# CZEKAJ_PROBY odczytów (domyślnie 12) co CZEKAJ_SEKUNDY (domyślnie 10). Etykiety nadaje równolegle workflow
# `etykiety` tokenem workflow, a takie nadanie nie uruchamia kontroli „PR od issue” ponownie (#1086), więc kontrola
# czeka na nie sama. Zawsze kończy się kodem 0: brak etykiety po czasie zgłosi pr-od-issue.sh.
pr="${1:?Podaj numer PR}"
proby="${CZEKAJ_PROBY:-12}"
odstep="${CZEKAJ_SEKUNDY:-10}"

labeler="$(dirname "${BASH_SOURCE[0]}")/../labeler.yml"
mapfile -t areas < <(grep -oP '^[a-z][a-z-]*(?=:)' "$labeler" 2>/dev/null || true)
if ((${#areas[@]} == 0)); then
  echo "Brak etykiet obszaru w ${labeler}; kontrola bez czekania."
  exit 0
fi

for ((i = 1; i <= proby; i++)); do
  names="$(gh pr view "$pr" --json labels --jq '.labels[].name' 2>/dev/null || true)"
  while IFS= read -r name; do
    if [[ -n $name && " ${areas[*]} " == *" ${name} "* ]]; then
      echo "PR #${pr} ma etykietę obszaru ${name}."
      exit 0
    fi
  done <<<"$names"
  if ((i < proby)); then
    sleep "$odstep"
  fi
done
echo "PR #${pr} nadal bez etykiety obszaru po ${proby} odczytach; kontrola zgłosi brak."
