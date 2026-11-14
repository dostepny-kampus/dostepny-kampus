#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: pr-od-issue.sh <numer PR>. Każdy PR (poza Dependabotem) musi mieć w opisie słowo zamykające (Closes, Fixed, Resolves … #N),
# a nazwa gałęzi PR ma mieć postać N-krotki-opis, gdzie #N jest wśród zamykanych issue,
# PR ma mieć etykietę obszaru (android, backend, api, ci, test, documentation), a jeśli zamykane issue ma etykiety
# obszaru, PR ma mieć co najmniej jedną z nich; każde wskazane issue ma być otwarte i przypisane autorowi PR. Opisu szukamy sami, bo dla PR do gałęzi
# niedomyślnej GitHub nie wypełnia closingIssuesReferences. PR do gałęzi sprint/N zamyka tylko issue z kamieniem
# milowym „Sprint N”, żeby zadania następnego sprintu nie trafiały do bieżącego.
# Kod wyjścia: 0 = poprawnie, 1 = naruszenie, 2 = błąd narzędzia gh lub jq (nie wiadomo, czy PR jest w porządku).
# Tryb ostrzegawczy: PR_OD_ISSUE_OSTRZEGAJ=1 zgłasza te same uwagi jako ::warning i kończy kodem 0.
# Dane z gh; w teście zamiast sieci: PR_JSON_FILE (gh pr view --json author,baseRefName,body,headRefName,isCrossRepository,labels)
# i ISSUE_JSON_DIR (pliki <numer>.json z gh issue view --json state,assignees,url,labels,milestone).
pr="${1:?Podaj numer PR}"

tool_error() {
  echo "::error title=Błąd narzędzia::błąd narzędzia $1: $2"
  exit 2
}

# Dane PR w $pr_data, dane issue w $data (pusto = issue nie istnieje). Bez podpowłoki, żeby komunikat błędu narzędzia
# trafiał do logu, a nie do przechwyconego wyniku.
pr_data=''
load_pr() {
  if [[ -n ${PR_JSON_FILE:-} ]]; then
    pr_data="$(cat "$PR_JSON_FILE")"
  else
    pr_data="$(gh pr view "$pr" --json author,baseRefName,body,headRefName,isCrossRepository,labels)" || tool_error gh "gh pr view ${pr}"
  fi
}

data=''
err_file=''
trap 'rm -f "$err_file"' EXIT
load_issue() {
  data=''
  if [[ -n ${ISSUE_JSON_DIR:-} ]]; then
    if [[ -f $ISSUE_JSON_DIR/$1.json ]]; then
      data="$(cat "$ISSUE_JSON_DIR/$1.json")"
    fi
    return
  fi
  err_file="$(mktemp)"
  if ! data="$(gh issue view "$1" --json state,assignees,url,labels,milestone 2>"$err_file")"; then
    data=''
    if ! grep -qi 'could not resolve to an issue' "$err_file"; then
      tool_error gh "gh issue view ${1}: $(head -n 1 "$err_file")"
    fi
  fi
  rm -f "$err_file"
}

# Obszary to etykiety z .github/labeler.yml (jedno źródło; etykiety nadaje labeler według zmienionych plików).
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

load_pr
author="$(jq -r '.author.login' <<<"$pr_data")" || tool_error jq "nie odczytano autora PR"
if [[ $author == 'dependabot[bot]' ]]; then
  echo "PR Dependabota: kontrola pominięta."
  exit 0
fi

base="$(jq -r '.baseRefName // ""' <<<"$pr_data")" || tool_error jq "nie odczytano gałęzi docelowej PR"
branch="$(jq -r '.headRefName // ""' <<<"$pr_data")" || tool_error jq "nie odczytano gałęzi PR"
if [[ $base == main && $branch =~ ^sprint/[0-9]+$ ]]; then
  cross_repository="$(jq -r '.isCrossRepository' <<<"$pr_data")" || tool_error jq "nie odczytano źródła PR"
  if [[ $cross_repository == false ]]; then
    echo "Scalenie sprintu do main: kontrolę powiązania z issue wykonano w PR do gałęzi sprintu."
    exit 0
  fi
fi

# shellcheck source=slowa-zamykajace.sh
source "$(dirname "${BASH_SOURCE[0]}")/slowa-zamykajace.sh"
body="$(jq -r '.body // ""' <<<"$pr_data")" || tool_error jq "nie odczytano opisu PR"
issues="$(numery_zamykanych_issue <<<"$body")"
if [[ -z $issues ]]; then
  report "brak Closes: opis PR nie zawiera słowa zamykającego (np. Closes #N)."
  if [[ $level == warning ]]; then
    exit 0
  fi
  exit 1
fi

if [[ ! $branch =~ ^([0-9]+)- ]]; then
  report "gałąź ${branch:-<brak>}: nazwa gałęzi nie zaczyna się od numeru issue (N-krotki-opis)."
elif ! grep -qx "${BASH_REMATCH[1]}" <<<"$issues"; then
  report "gałąź ${branch}: numer #${BASH_REMATCH[1]} nie jest wśród zamykanych issue ($(printf '#%s ' $issues))."
fi

sprint=''
if [[ $base =~ ^sprint/([0-9]+)$ ]]; then
  sprint="Sprint ${BASH_REMATCH[1]}"
fi

area_labels "$pr_data"
pr_areas="$area_result"
if [[ -z $pr_areas ]]; then
  report "PR nie ma etykiety obszaru (${areas[*]})."
fi

for issue in $issues; do
  load_issue "$issue"
  if [[ -z $data ]]; then
    report "#${issue}: issue nie istnieje."
    continue
  fi
  url="$(jq -r '.url // ""' <<<"$data")" || tool_error jq "nie odczytano url issue #${issue}"
  state="$(jq -r '.state' <<<"$data")" || tool_error jq "nie odczytano stanu issue #${issue}"
  # gh issue view zwraca też dane PR; numer PR nie jest issue.
  if [[ $url == */pull/* ]]; then
    report "#${issue}: to PR, nie issue."
  elif [[ $state != 'OPEN' ]]; then
    report "#${issue}: issue zamknięte."
  else
    rc=0
    jq -e --arg login "$author" '[.assignees[].login] | index($login)' <<<"$data" >/dev/null || rc=$?
    # jq -e: 1 = brak wyniku (autor nie jest przypisany), inny kod ≠ 0 = błąd jq.
    if ((rc == 1)); then
      report "#${issue}: issue nieprzypisane autorowi (${author})."
    elif ((rc != 0)); then
      tool_error jq "nie odczytano osób przypisanych do issue #${issue}"
    fi
  fi
  if [[ -n $sprint ]]; then
    stone="$(jq -r '.milestone.title // ""' <<<"$data")" || tool_error jq "nie odczytano kamienia milowego issue #${issue}"
    if [[ $stone != "$sprint" ]]; then
      report "#${issue}: kamień milowy issue (${stone:-brak}) różni się od gałęzi ${base} (${sprint})."
    fi
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
