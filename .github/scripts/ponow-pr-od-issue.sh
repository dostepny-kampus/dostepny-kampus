#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: ponow-pr-od-issue.sh <numer issue>. Ponawia nieudane zadania ostatniego przebiegu kontroli „PR od issue”
# na głowie każdego otwartego PR, który w opisie zamyka to issue (słowa z slowa-zamykajace.sh). Kontrola czyta
# etykietę złożoności issue w chwili zdarzenia PR, a nadanie etykiety issue nie uruchamia jej ponownie.
# Przebieg udany, trwający albo nieobecny zostaje bez zmian. Kończy się kodem 0 także przy błędzie gh dla jednego PR.
issue="${1:?Podaj numer issue}"

# shellcheck source=slowa-zamykajace.sh
source "$(dirname "${BASH_SOURCE[0]}")/slowa-zamykajace.sh"

prs="$(gh pr list --state open --limit 200 --json number --jq '.[].number')"
while IFS= read -r pr; do
  [[ -z $pr ]] && continue
  body="$(gh pr view "$pr" --json body --jq '.body // ""' 2>/dev/null || true)"
  if ! numery_zamykanych_issue <<<"$body" | grep -qxF -- "$issue"; then
    continue
  fi
  head="$(gh pr view "$pr" --json headRefOid --jq '.headRefOid' 2>/dev/null || true)"
  run="$(gh run list --workflow pr-od-issue.yml --commit "$head" --limit 1 \
    --json databaseId,status,conclusion \
    --jq '.[] | select(.status == "completed" and .conclusion == "failure") | .databaseId' 2>/dev/null || true)"
  if [[ -z $run ]]; then
    echo "PR #${pr}: ostatni przebieg „PR od issue” nie jest nieudany, bez ponowienia."
    continue
  fi
  if gh run rerun "$run" --failed >/dev/null 2>&1; then
    echo "PR #${pr}: ponowiono nieudane zadania przebiegu ${run}."
  else
    echo "::warning title=PR od issue::PR #${pr}: nie udało się ponowić przebiegu ${run}."
  fi
done <<<"$prs"
