#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Zapisuje wynik kontroli sprintu jako status `kontrola-sprintu` commita SHA i prowadzi jedno issue czerwonego sprintu
# dla gałęzi GALAZ (etykieta `sprint-czerwony`, tytuł z nazwą gałęzi).
# - WYNIK=success: status success; otwarte issue tej gałęzi dostaje komentarz z commitem i jest zamykane.
# - inny WYNIK (także pierwsza porażka): status failure; otwarte issue dostaje komentarz, inaczej powstaje nowe
#   z nieudanymi zadaniami przebiegu i PR scalonymi od commita OD (ostatni wcześniejszy wynik) jako podejrzanymi.
#   Bez czekania na drugą porażkę: głowa sprintu zmienia się między przebiegami, a czerwień ma być widoczna od razu.
# Środowisko: GITHUB_REPOSITORY, GALAZ, SHA, OD (może być puste), WYNIK, RUN_ID, ADRES (link do przebiegu).
repo="$GITHUB_REPOSITORY"
krotki="${SHA:0:7}"

ustaw() {
  gh api -X POST "repos/${repo}/statuses/${SHA}" -f state="$1" -f context=kontrola-sprintu -f description="$2" \
    -f target_url="$ADRES" >/dev/null
  echo "${krotki}: ${1} — ${2}"
}

issue="$(gh issue list --label sprint-czerwony --state open --search "\"${GALAZ}\" in:title" \
  --json number --jq '.[0].number // empty')"

if [[ $WYNIK == success ]]; then
  ustaw success "Zielona ciężka kontrola ${GALAZ}"
  if [[ -n $issue ]]; then
    gh issue close "$issue" --comment "Kontrola sprintu ${GALAZ} zielona na ${SHA}: ${ADRES}"
  fi
  exit 0
fi

ustaw failure "Czerwona ciężka kontrola ${GALAZ} (${WYNIK})"
nieudane="$(gh api "repos/${repo}/actions/runs/${RUN_ID}/jobs?per_page=100" \
  --jq '.jobs[] | select(.conclusion == "failure" or .conclusion == "cancelled") | "- `\(.name)`"')"
if [[ -n ${OD:-} ]]; then
  zakres="${OD:0:7}..${krotki}"
  podejrzane="$(git log --format=%s "${OD}..${SHA}" | grep -oE '\(#[0-9]+\)$' | tr -d '()' | sed 's/^/- /' || true)"
else
  zakres="brak wcześniejszego wyniku, cała historia do ${krotki}"
  podejrzane=''
fi

if [[ -n $issue ]]; then
  gh issue comment "$issue" --body "Kontrola sprintu ${GALAZ} nadal czerwona na ${SHA}: ${ADRES}
Nieudane zadania:
${nieudane:-- brak danych}"
  exit 0
fi

tresc="$(mktemp)"
cat >"$tresc" <<EOF
### Problem

Ciężka kontrola sprintu (APK, emulatory, pełny test systemowy, buildHealth) jest czerwona na \`${GALAZ}\` na commicie ${SHA}: ${ADRES}

Nieudane zadania:
${nieudane:-- brak danych}

Zakres zmian od ostatniego wyniku: ${zakres}. PR scalone w tym zakresie:
${podejrzane:-- brak}

### Jak odtworzyć

Ręczne uruchomienie workflow \`kontrola-sprintu\` z gałęzią \`${GALAZ}\` albo log przebiegu powyżej.

### Oczekiwane

Kontrola sprintu na głowie \`${GALAZ}\` jest zielona; to issue zamyka się samo przy zielonym wyniku.

### Gotowe, gdy

- [ ] przyczyna wskazana w komentarzu (PR albo niestabilny test z issue),
- [ ] poprawka scalona przez kolejkę na początku (\`jump\`).

### Rozwiązanie

do wyboru spośród znanych

### Zasięg

jeden moduł

### Weryfikacja

CI sprawdza w pełni

### Ryzyko

brak

### Odniesienia

Przebieg ${ADRES}; podejrzane PR wyżej.

### Warianty i otwarte pytania

Cofnięcie podejrzanego PR albo poprawka w przód; wybór po przeczytaniu logu.
EOF
# Issue założone tokenem przebiegu nie uruchamia workflow etykiet złożoności, więc poziom z pól wyżej nadaje skrypt.
gh issue create --title "Czerwona kontrola sprintu: ${GALAZ}" --label sprint-czerwony --label ci \
  --label "złożoność: 3 wymaga analizy" --milestone "Sprint ${GALAZ#sprint/}" --body-file "$tresc"
rm -f "$tresc"
