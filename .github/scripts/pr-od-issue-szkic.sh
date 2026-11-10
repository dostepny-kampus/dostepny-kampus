#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: pr-od-issue-szkic.sh <numer PR>. Środowisko: PR_OD_ISSUE_WYMAGANE (1 = tryb wymagany), GITHUB_REPOSITORY.
# Uruchamia kontrolę „PR od issue” (pr-od-issue.sh, ten sam katalog). Domyślnie (tryb ostrzegawczy) tylko loguje
# uwagi jako ostrzeżenia. W trybie wymaganym przy naruszeniu dodaje (albo odświeża) komentarz
# z listą braków, a skrypt kończy się kodem 1; PR nie jest zamieniany w szkic (token workflow nie ma do tego prawa).
# PR Dependabota kontrola pomija.
pr="${1:?Podaj numer PR}"
wymagane="${PR_OD_ISSUE_WYMAGANE:-0}"
repo="${GITHUB_REPOSITORY:?Brak GITHUB_REPOSITORY}"
dir="$(cd "$(dirname "$0")" && pwd)"
znacznik='<!-- pr-od-issue -->'

status=0
wynik="$(PR_OD_ISSUE_OSTRZEGAJ=0 bash "$dir/pr-od-issue.sh" "$pr" 2>&1)" || status=$?
grep -v '^::' <<<"$wynik" || true
if ((status == 0)); then
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

zakonczenie='Dopisz w opisie `Closes #N` z otwartym issue przypisanym autorowi PR; kontrola powtórzy się po zmianie opisu.'
tresc="${znacznik}
Ten PR nie spełnia zasady „PR od issue”:
$(sed 's/^/- /' <<<"$uwagi")

${zakonczenie}"
narzedzie() {
  echo "::error title=PR od issue::Błąd narzędzia kontroli: $1 nie powiodło się (kod $2). Kod 2."
  exit 2
}
id="$(gh api "repos/${repo}/issues/${pr}/comments" --jq ".[] | select(.body | contains(\"${znacznik}\")) | .id" | head -n 1)" ||
  narzedzie "odczyt komentarzy" "$?"
if [[ -n $id ]]; then
  gh api -X PATCH "repos/${repo}/issues/comments/${id}" -f body="$tresc" >/dev/null || narzedzie "odświeżenie komentarza" "$?"
  echo "Odświeżono komentarz ${id}."
else
  gh pr comment "$pr" --body "$tresc" || narzedzie "dodanie komentarza" "$?"
fi
exit 1
