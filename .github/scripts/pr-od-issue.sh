#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: pr-od-issue.sh <numer PR>. Każdy PR (poza Dependabotem) musi mieć w opisie Closes|Fixes|Resolves #N,
# a nazwa gałęzi PR ma mieć postać N-krotki-opis, gdzie #N jest wśród zamykanych issue (poza dependabot/*),
# a każde wskazane issue ma być otwarte i przypisane autorowi PR. Opisu szukamy sami, bo dla PR do gałęzi
# niedomyślnej GitHub nie wypełnia closingIssuesReferences.
# Tryb ostrzegawczy: PR_OD_ISSUE_OSTRZEGAJ=1 zgłasza te same uwagi jako ::warning i kończy kodem 0.
# Dane z gh; w teście zamiast sieci: PR_JSON_FILE (gh pr view --json author,body,headRefName)
# i ISSUE_JSON_DIR (pliki <numer>.json z gh issue view --json state,assignees,url).
pr="${1:?Podaj numer PR}"

pr_json() {
  if [[ -n ${PR_JSON_FILE:-} ]]; then
    cat "$PR_JSON_FILE"
  else
    gh pr view "$pr" --json author,body,headRefName
  fi
}

issue_json() {
  if [[ -n ${ISSUE_JSON_DIR:-} ]]; then
    cat "$ISSUE_JSON_DIR/$1.json"
  else
    gh issue view "$1" --json state,assignees,url
  fi
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

# Closes|Fixes|Resolves #N, wielkość liter bez znaczenia; słowo kluczowe nie może być końcem innego słowa.
issues="$(jq -r '.body // ""' <<<"$pr_data" | grep -oiP '(?<!\w)(?:closes|fixes|resolves)\s+#\K\d+' | sort -un || true)"
if [[ -z $issues ]]; then
  report "brak Closes: opis PR nie zawiera Closes/Fixes/Resolves #N."
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
done

if ((found == 0)); then
  echo "PR zamyka otwarte issue przypisane autorowi: $(printf '#%s ' $issues)"
fi
if [[ $level == warning ]]; then
  exit 0
fi
exit "$found"
