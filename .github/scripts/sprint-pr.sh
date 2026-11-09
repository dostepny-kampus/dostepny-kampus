#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: sprint-pr.sh <numer PR>. Środowisko: BASE_REF (gałąź bazowa PR), PR_AUTHOR, GITHUB_REPOSITORY.
# PR do `sprint/N` dostaje kamień milowy „Sprint N” i, jeśli nikt nie jest przypisany, swojego autora.
# Pola ustawione ręcznie zostają. Dla PR Dependabota ustawiamy tylko kamień milowy.
pr="${1:?Podaj numer PR}"
base="${BASE_REF:?Brak BASE_REF}"
author="${PR_AUTHOR:-}"

if [[ ! $base =~ ^sprint/([0-9]+)$ ]]; then
  echo "PR #${pr} nie jest do gałęzi sprintu (${base}), nic do ustawienia."
  exit 0
fi
milestone="Sprint ${BASH_REMATCH[1]}"

current="$(gh pr view "$pr" --json milestone --jq '.milestone.title // ""')"
if [[ -n $current ]]; then
  echo "Kamień milowy już ustawiony: ${current}, bez zmian."
else
  titles="$(gh api "repos/${GITHUB_REPOSITORY}/milestones?state=all&per_page=100" --jq '.[].title')"
  if grep -qxF "$milestone" <<<"$titles"; then
    gh pr edit "$pr" --milestone "$milestone"
    echo "Ustawiono kamień milowy „${milestone}”."
  else
    echo "::warning title=Kamień milowy::Brak kamienia milowego „${milestone}” — PR #${pr} zostaje bez niego."
  fi
fi

assignees="$(gh pr view "$pr" --json assignees --jq '.assignees | length')"
if [[ $author == 'dependabot[bot]' ]]; then
  echo "PR Dependabota: osoby nie przypisujemy."
elif ((assignees > 0)); then
  echo "Osoba już przypisana, bez zmian."
elif [[ -n $author ]]; then
  gh pr edit "$pr" --add-assignee "$author"
  echo "Przypisano autora: ${author}."
fi
