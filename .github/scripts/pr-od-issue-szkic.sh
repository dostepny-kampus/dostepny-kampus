#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: pr-od-issue-szkic.sh <numer PR>. Środowisko: PR_OD_ISSUE_WYMAGANE (1 = tryb wymagany), GITHUB_REPOSITORY.
# Uruchamia kontrolę „PR od issue” (pr-od-issue.sh, ten sam katalog). Domyślnie (tryb ostrzegawczy) tylko loguje
# uwagi jako ostrzeżenia. W trybie wymaganym przy naruszeniu zamienia PR w szkic i dodaje (albo odświeża) komentarz
# z listą braków, a skrypt kończy się kodem 1. PR Dependabota kontrola pomija.
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
# brak danych) to błąd narzędzia: nie ruszamy PR i kończymy kodem 2, żeby nie robić szkicu z pustą listą braków.
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

if [[ "$(gh pr view "$pr" --json isDraft --jq .isDraft)" != true ]]; then
  # Token workflow (GITHUB_TOKEN) bywa bez prawa do zamiany PR w szkic (GraphQL: convertPullRequestToDraft); wtedy
  # zostaje komentarz i czerwony wynik zadania, a ostrzeżenie mówi, czemu PR nie jest szkicem.
  if gh pr ready "$pr" --undo; then
    echo "PR #${pr} zamieniony w szkic."
  else
    echo "::warning title=PR od issue::Nie udało się zamienić PR #${pr} w szkic (brak uprawnienia tokenu); zostaje komentarz i czerwony wynik."
  fi
fi

tresc="${znacznik}
Ten PR nie spełnia zasady „PR od issue” i został zamieniony w szkic:
$(sed 's/^/- /' <<<"$uwagi")

Dopisz w opisie \`Closes #N\` z otwartym issue przypisanym autorowi PR, potem oznacz PR jako gotowy."
id="$(gh api "repos/${repo}/issues/${pr}/comments" --jq ".[] | select(.body | contains(\"${znacznik}\")) | .id" | head -n 1)"
if [[ -n $id ]]; then
  gh api -X PATCH "repos/${repo}/issues/comments/${id}" -f body="$tresc" >/dev/null
  echo "Odświeżono komentarz ${id}."
else
  gh pr comment "$pr" --body "$tresc"
fi
exit 1
