#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Wybiera commit dla kontroli sprintu. Bez GALAZ bierze gałęzie `origin/sprint/N`, których głowy nie ma w
# `origin/main`, od najniższego N. Głowę ze statusem `kontrola-sprintu` success, failure albo świeżym `pending`
# pomija; `pending` starszy niż 3 godziny (przerwany przebieg) i `error` (pierwsza porażka) sprawdza znowu. Gdy od ostatniego commita ze statusem success albo failure
# nic się nie zmieniło w obszarze Androida i serwera, stawia success bez budowy i idzie dalej. Pierwszą głowę
# do sprawdzenia oznacza `pending` i zapisuje do $GITHUB_OUTPUT: jest=true, galaz, sha, od; inaczej jest=false.
# GALAZ wymusza sprawdzenie tej gałęzi bez względu na status i zmiany. Środowisko: GITHUB_REPOSITORY,
# GITHUB_OUTPUT, opcjonalnie GALAZ, ADRES (link do przebiegu), TERAZ (sekundy epoki, do testu).
kontekst=kontrola-sprintu
obszar=(android backend infra data/buildings api/openapi.yaml docs/docs/ux/tokeny.yml .github/actions/android-gradle
  .github/workflows/android.yml .github/workflows/kontrola-sprintu.yml .github/scripts/pakuj-serwer-systemowy.sh)
teraz="${TERAZ:-$(date -u +%s)}"

stan() {
  gh api "repos/${GITHUB_REPOSITORY}/commits/$1/status" \
    --jq "[.statuses[] | select(.context == \"${kontekst}\")][0] | select(.) | \"\(.state) \(.updated_at)\""
}

ustaw() {
  gh api -X POST "repos/${GITHUB_REPOSITORY}/statuses/$1" -f state="$2" -f context="$kontekst" \
    -f description="$3" ${ADRES:+-f target_url="$ADRES"} >/dev/null
  echo "${1:0:7}: ${2} — ${3}"
}

if [[ -n ${GALAZ:-} ]]; then
  if ! git rev-parse -q --verify "refs/remotes/origin/${GALAZ}" >/dev/null; then
    echo "::error title=Kontrola sprintu::Brak gałęzi ${GALAZ}."
    exit 1
  fi
  galezie=("$GALAZ")
else
  mapfile -t galezie < <(git for-each-ref --format='%(refname:lstrip=3)' refs/remotes/origin/sprint/ |
    grep -E '^sprint/[0-9]+$' | sort -t/ -k2 -n || true)
fi

for galaz in "${galezie[@]}"; do
  sha="$(git rev-parse "refs/remotes/origin/${galaz}")"
  state=''
  if [[ -z ${GALAZ:-} ]]; then
    if git merge-base --is-ancestor "$sha" refs/remotes/origin/main; then
      echo "${galaz}: głowa jest już w main, pomijam."
      continue
    fi
    wpis="$(stan "$sha")"
    state="${wpis%% *}"
    if [[ $state == success || $state == failure ]] ||
      [[ $state == pending && $((teraz - $(date -u -d "${wpis#* }" +%s))) -lt 10800 ]]; then
      echo "${galaz}: ${sha:0:7} ma już status ${state}, pomijam."
      continue
    fi
  fi
  ponowienie="$([[ ${state:-} == error ]] && echo tak || true)"
  od=''
  for commit in $(git rev-list --first-parent --skip=1 --max-count=50 "$sha"); do
    state="$(stan "$commit")"
    if [[ ${state%% *} == success || ${state%% *} == failure ]]; then
      od="$commit"
      break
    fi
  done
  if [[ -z ${GALAZ:-} && -z $ponowienie && -n $od ]] && git diff --quiet "$od" "$sha" -- "${obszar[@]}"; then
    ustaw "$sha" success "Bez zmian Androida i serwera od ${od:0:7}"
    continue
  fi
  ustaw "$sha" pending "Sprawdzanie ${galaz} w toku"
  {
    echo "jest=true"
    echo "galaz=${galaz}"
    echo "sha=${sha}"
    echo "od=${od}"
  } >>"$GITHUB_OUTPUT"
  exit 0
done
echo "jest=false" >>"$GITHUB_OUTPUT"
echo "Brak głowy sprintu do sprawdzenia."
