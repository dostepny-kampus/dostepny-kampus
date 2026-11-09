#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: pr-od-issue.sh <numer PR>. Każdy PR (poza Dependabotem) musi mieć w opisie słowo zamykające (Closes, Fixed, Resolves … #N),
# a nazwa gałęzi PR ma mieć postać N-krotki-opis, gdzie #N jest wśród zamykanych issue (poza dependabot/*),
# PR ma mieć etykietę obszaru (android, backend, api, ci, test, documentation), a jeśli zamykane issue ma etykiety
# obszaru, PR ma mieć co najmniej jedną z nich; każde wskazane issue ma być otwarte i przypisane autorowi PR. Opisu szukamy sami, bo dla PR do gałęzi
# niedomyślnej GitHub nie wypełnia closingIssuesReferences.
# Tryb ostrzegawczy: PR_OD_ISSUE_OSTRZEGAJ=1 zgłasza te same uwagi jako ::warning i kończy kodem 0.
# Dane z gh; w teście zamiast sieci: PR_JSON_FILE (gh pr view --json author,body,headRefName,labels)
# i ISSUE_JSON_DIR (pliki <numer>.json z gh issue view --json state,assignees,url,labels).
pr="${1:?Podaj numer PR}"

pr_json() {
  if [[ -n ${PR_JSON_FILE:-} ]]; then
    cat "$PR_JSON_FILE"
  else
    gh pr view "$pr" --json author,body,headRefName,labels
  fi
}

issue_json() {
  if [[ -n ${ISSUE_JSON_DIR:-} ]]; then
    cat "$ISSUE_JSON_DIR/$1.json"
  else
    gh issue view "$1" --json state,assignees,url,labels
  fi
}

# Obszary to etykiety z .github/labeler.yml (jedno źródło; etykiety nadaje labeler według zmienionych plików).
tool_error() {
  echo "::error title=Błąd narzędzia::błąd narzędzia $1: $2"
  exit 2
}

labeler="$(dirname "${BASH_SOURCE[0]}")/../labeler.yml"
mapfile -t areas < <(grep -oP '^[a-z][a-z-]*(?=:)' "$labeler" 2>/dev/null || true)
if ((${#areas[@]} == 0)); then
  tool_error labeler "brak etykiet obszaru w ${labeler}"
fi

# area_labels <JSON z polem labels>: ustawia $area_result na etykiety obszaru rozdzielone spacjami.
# Bez podpowłoki, żeby błąd jq kończył skrypt.
area_result=''
area_labels() {
  local names name found=()
  names="$(jq -r '[(.labels // [])[].name] | join(" ")' <<<"$1")" || tool_error jq "nie odczytano etykiet"
  for name in $names; do
    if [[ " ${areas[*]} " == *" $name "* ]]; then
      found+=("$name")
    fi
  done
  area_result="${found[*]:-}"
}

level=error
if [[ ${PR_OD_ISSUE_OSTRZEGAJ:-0} == 1 ]]; then
  level=warning
fi
found=0

report() {
  echo "::${level} title=PR od issue::$1"
  found=1
}

pr_data="$(pr_json)"
author="$(jq -r '.author.login' <<<"$pr_data")"
if [[ $author == 'dependabot[bot]' ]]; then
  echo "PR Dependabota: kontrola pominięta."
  exit 0
fi

# shellcheck source=slowa-zamykajace.sh
source "$(dirname "${BASH_SOURCE[0]}")/slowa-zamykajace.sh"
issues="$(jq -r '.body // ""' <<<"$pr_data" | numery_zamykanych_issue)"
if [[ -z $issues ]]; then
  report "brak Closes: opis PR nie zawiera słowa zamykającego (np. Closes #N)."
  if [[ $level == warning ]]; then
    exit 0
  fi
  exit 1
fi

branch="$(jq -r '.headRefName // ""' <<<"$pr_data")"
if [[ $branch != dependabot/* ]]; then
  if [[ ! $branch =~ ^([0-9]+)- ]]; then
    report "gałąź ${branch:-<brak>}: nazwa gałęzi nie zaczyna się od numeru issue (N-krotki-opis)."
  elif ! grep -qx "${BASH_REMATCH[1]}" <<<"$issues"; then
    report "gałąź ${branch}: numer #${BASH_REMATCH[1]} nie jest wśród zamykanych issue ($(printf '#%s ' $issues))."
  fi
fi

area_labels "$pr_data"
pr_areas="$area_result"
if [[ -z $pr_areas ]]; then
  report "PR nie ma etykiety obszaru (${areas[*]})."
fi

for issue in $issues; do
  data="$(issue_json "$issue")"
  # gh issue view zwraca też dane PR; numer PR nie jest issue.
  if [[ "$(jq -r '.url // ""' <<<"$data")" == */pull/* ]]; then
    report "#${issue}: to PR, nie issue."
  elif [[ "$(jq -r '.state' <<<"$data")" != 'OPEN' ]]; then
    report "#${issue}: issue zamknięte."
  elif ! jq -e --arg login "$author" '[.assignees[].login] | index($login)' <<<"$data" >/dev/null; then
    report "#${issue}: issue nieprzypisane autorowi (${author})."
  fi
  area_labels "$data"
  issue_areas="$area_result"
  if [[ -n $issue_areas && -n $pr_areas ]]; then
    shared=0
    for name in $issue_areas; do
      if [[ " $pr_areas " == *" $name "* ]]; then
        shared=1
      fi
    done
    if ((shared == 0)); then
      report "#${issue}: etykiety obszaru issue (${issue_areas}) nie pasują do etykiet obszaru PR (${pr_areas})."
    fi
  fi
done

if ((found == 0)); then
  echo "PR zamyka otwarte issue przypisane autorowi: $(printf '#%s ' $issues)"
fi
if [[ $level == warning ]]; then
  exit 0
fi
exit "$found"
