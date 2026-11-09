#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: dependabot-sprint.sh <numer PR>. Środowisko: PR_AUTHOR, GITHUB_REPOSITORY.
# PR Dependabota dostaje bazę na najwyższą gałąź `sprint/N` i kamień milowy „Sprint N”. Dependabot czyta
# `dependabot.yml` tylko z gałęzi domyślnej, więc sam nie wie, który sprint jest bieżący.
# Brak gałęzi sprintu albo kamienia milowego to błąd (kod 1) i żadnej zmiany w PR.
pr="${1:?Podaj numer PR}"
author="${PR_AUTHOR:-}"

if [[ $author != 'dependabot[bot]' ]]; then
  echo "PR #${pr} nie jest PR Dependabota (autor: ${author:-brak}), nic do ustawienia."
  exit 0
fi

branches="$(gh api "repos/${GITHUB_REPOSITORY}/branches?per_page=100" --paginate --jq '.[].name')"
number="$(grep -E '^sprint/[0-9]+$' <<<"$branches" | sed 's|^sprint/||' | sort -n | tail -n 1 || true)"
if [[ -z $number ]]; then
  echo "::error title=Dependabot::Brak gałęzi sprintu (sprint/N) — PR #${pr} zostaje bez zmian."
  exit 1
fi

milestone="Sprint ${number}"
titles="$(gh api "repos/${GITHUB_REPOSITORY}/milestones?state=all&per_page=100" --jq '.[].title')"
if ! grep -qxF "$milestone" <<<"$titles"; then
  echo "::error title=Dependabot::Brak kamienia milowego „${milestone}” — PR #${pr} zostaje bez zmian."
  exit 1
fi

gh pr edit "$pr" --base "sprint/${number}" --milestone "$milestone"
echo "PR #${pr}: baza sprint/${number}, kamień milowy „${milestone}”."
