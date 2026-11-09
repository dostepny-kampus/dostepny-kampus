#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: pr-od-issue.sh <numer PR>. Każdy PR (poza Dependabotem) musi mieć w opisie Closes|Fixes|Resolves #N,
# a każde wskazane issue ma być otwarte i przypisane autorowi PR. Opisu szukamy sami, bo dla PR do gałęzi
# niedomyślnej GitHub nie wypełnia closingIssuesReferences.
# Dane z gh; w teście zamiast sieci: PR_JSON_FILE (gh pr view --json author,body)
# i ISSUE_JSON_DIR (pliki <numer>.json z gh issue view --json state,assignees).
pr="${1:?Podaj numer PR}"

pr_json() {
  if [[ -n ${PR_JSON_FILE:-} ]]; then
    cat "$PR_JSON_FILE"
  else
    gh pr view "$pr" --json author,body
  fi
}

issue_json() {
  if [[ -n ${ISSUE_JSON_DIR:-} ]]; then
    cat "$ISSUE_JSON_DIR/$1.json"
  else
    gh issue view "$1" --json state,assignees
  fi
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
  echo "::error title=PR od issue::brak Closes: opis PR nie zawiera Closes/Fixes/Resolves #N."
  exit 1
fi

found=0
for issue in $issues; do
  data="$(issue_json "$issue")"
  if [[ "$(jq -r '.state' <<<"$data")" != 'OPEN' ]]; then
    echo "::error title=PR od issue::#${issue}: issue zamknięte."
    found=1
  elif ! jq -e --arg login "$author" '[.assignees[].login] | index($login)' <<<"$data" >/dev/null; then
    echo "::error title=PR od issue::#${issue}: issue nieprzypisane autorowi (${author})."
    found=1
  fi
done

if ((found == 0)); then
  echo "PR zamyka otwarte issue przypisane autorowi: $(printf '#%s ' $issues)"
fi
exit "$found"
