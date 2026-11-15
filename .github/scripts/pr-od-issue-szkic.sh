#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: pr-od-issue-szkic.sh <numer PR>. Środowisko: PR_OD_ISSUE_WYMAGANE (0 = tryb ostrzegawczy, domyślnie
# wymagany), GITHUB_REPOSITORY. Uruchamia kontrolę „PR od issue” (pr-od-issue.sh, ten sam katalog). Tryb ostrzegawczy
# tylko loguje uwagi jako ostrzeżenia. W trybie wymaganym przy naruszeniu dodaje (albo odświeża) komentarz
# z listą braków i wskazówką dla każdego z nich, a skrypt kończy się kodem 1; PR nie jest zamieniany w szkic
# (token workflow nie ma do tego prawa). Gdy PR przechodzi kontrolę, wcześniejszy komentarz bota ze znacznikiem
# jest usuwany, więc komentarz odpowiada bieżącemu stanowi (#1353). Przed komentarzem kontrola powtarza się jeszcze raz,
# żeby równoległy, nowszy przebieg nie zostawił starego ostrzeżenia. Komentarze innych autorów zostają nietknięte.
# PR Dependabota kontrola pomija.
pr="${1:?Podaj numer PR}"
wymagane="${PR_OD_ISSUE_WYMAGANE:-1}"
repo="${GITHUB_REPOSITORY:?Brak GITHUB_REPOSITORY}"
dir="$(cd "$(dirname "$0")" && pwd)"
znacznik='<!-- pr-od-issue -->'
autor_komentarza='github-actions[bot]'

narzedzie() {
  echo "::error title=PR od issue::Błąd narzędzia kontroli: $1 nie powiodło się (kod $2). Kod 2."
  exit 2
}

# Wynik kontroli w $wynik i $status.
status=0
wynik=''
ocen() {
  status=0
  wynik="$(PR_OD_ISSUE_OSTRZEGAJ=0 bash "$dir/pr-od-issue.sh" "$pr" 2>&1)" || status=$?
}

# Id komentarza bota ze znacznikiem (pusty, gdy go nie ma); komentarze innych autorów się nie liczą.
id_komentarza() {
  gh api "repos/${repo}/issues/${pr}/comments" \
    --jq ".[] | select(.user.login == \"${autor_komentarza}\" and (.body | contains(\"${znacznik}\"))) | .id" | head -n 1
}

# Po sukcesie komentarz bota z poprzedniego naruszenia znika: opisywał stan, którego już nie ma.
usun_stary_komentarz() {
  local id
  id="$(id_komentarza)" || narzedzie "odczyt komentarzy" "$?"
  if [[ -n $id ]]; then
    gh api -X DELETE "repos/${repo}/issues/comments/${id}" >/dev/null || narzedzie "usunięcie komentarza" "$?"
    echo "Usunięto nieaktualny komentarz ${id}."
  fi
}

# Wskazówka dla uwagi kontroli; nie każda uwaga dotyczy Closes.
wskazowka() {
  case "$1" in
    'brak Closes'*) echo 'Dopisz w opisie `Closes #N` z otwartym issue przypisanym autorowi PR.' ;;
    'gałąź '*) echo 'Nazwij gałąź N-krotki-opis, gdzie N jest jednym z zamykanych issue.' ;;
    'PR nie ma etykiety obszaru'*) echo 'Nadaj PR etykietę obszaru; zwykle robi to labeler według zmienionych plików.' ;;
    *'nieprzypisane autorowi'*) echo 'Przypisz issue autorowi PR.' ;;
    *'kamień milowy'*) echo 'Ustaw w issue kamień milowy zgodny z gałęzią docelową PR.' ;;
    *'etykiet złożoności'* | *'do podziału'*)
      echo 'Etykietę złożoności nadaje bot po uzupełnieniu sekcji issue (albo rozbij issue); potem zmień opis PR, żeby kontrola ruszyła ponownie.' ;;
    *'etykiety obszaru issue'*) echo 'Dopasuj etykiety obszaru PR do etykiet obszaru issue.' ;;
    *'issue nie istnieje'* | *'to PR, nie issue'* | *'issue zamknięte'*) echo 'Wskaż w opisie (Closes #N) otwarte issue przypisane autorowi PR.' ;;
    *) echo 'Popraw to, co opisuje uwaga.' ;;
  esac
}

ocen
if ((status == 1)) && [[ $wymagane == 1 ]]; then
  # Powtórka tuż przed komentarzem: równoległy przebieg mógł już zobaczyć poprawiony stan.
  ocen
fi
grep -v '^::' <<<"$wynik" || true
if ((status == 0)); then
  if [[ $wymagane == 1 ]]; then
    usun_stary_komentarz
  fi
  exit 0
fi

# Naruszenie zasady to kod 1 z co najmniej jedną uwagą „::error title=PR od issue”. Każdy inny przypadek (błąd gh, jq,
# brak danych) to błąd narzędzia: nie ruszamy PR i kończymy kodem 2, żeby nie komentować pustą listą braków.
uwagi="$(sed -n 's/^::error title=PR od issue:://p' <<<"$wynik")"
if ((status != 1)) || [[ -z $uwagi ]]; then
  echo "::error title=PR od issue::Błąd narzędzia kontroli (kod ${status}): nie udało się odczytać danych PR #${pr} lub issue. PR bez zmian."
  grep '^::' <<<"$wynik" | head -n 5 || true
  exit 2
fi
if [[ $wymagane != 1 ]]; then
  while IFS= read -r uwaga; do
    echo "::warning title=PR od issue::${uwaga} (tryb ostrzegawczy: PR zostaje bez zmian)"
  done <<<"$uwagi"
  exit 0
fi

while IFS= read -r uwaga; do
  echo "::error title=PR od issue::${uwaga}"
done <<<"$uwagi"

kroki=''
while IFS= read -r uwaga; do
  kroki+="$(wskazowka "$uwaga")"$'\n'
done <<<"$uwagi"
kroki="$(sort -u <<<"$kroki" | sed '/^$/d; s/^/- /')"
tresc="${znacznik}
Ten PR nie spełnia zasady „PR od issue”:
$(sed 's/^/- /' <<<"$uwagi")

Co zrobić:
${kroki}

Kontrola powtórzy się po zmianie opisu lub etykiet PR."
id="$(id_komentarza)" || narzedzie "odczyt komentarzy" "$?"
if [[ -n $id ]]; then
  gh api -X PATCH "repos/${repo}/issues/comments/${id}" -f body="$tresc" >/dev/null || narzedzie "odświeżenie komentarza" "$?"
  echo "Odświeżono komentarz ${id}."
else
  gh pr comment "$pr" --body "$tresc" || narzedzie "dodanie komentarza" "$?"
fi
exit 1
