#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: drzewo-grupy.sh zapisz | porownaj
# zapisz: na PR zapisuje git tree SHA commitu scalenia (GITHUB_SHA) do $DRZEWO_KATALOG/drzewo.txt (artefakt przebiegu).
# porownaj: w grupie kolejki porównuje drzewo commitu grupy (GROUP_HEAD_SHA, GROUP_HEAD_REF) z drzewem, na którym ostatni
# przebieg workflow DRZEWO_WORKFLOW na PR był w pełni zielony; wynik w GITHUB_OUTPUT: `pomin=true` albo `pomin=false`.
# Zmiana katalogu .github względem GROUP_BASE_SHA (workflow, skrypty) zawsze daje `pomin=false`. Każdy błąd odczytu też,
# więc niepewność nigdy nie osłabia kontroli. Środowisko: GH_TOKEN, GH_REPO.
tryb="${1:?Podaj tryb: zapisz albo porownaj}"

odpowiedz() {
  echo "pomin=$1" >>"$GITHUB_OUTPUT"
  echo "$2" >>"${GITHUB_STEP_SUMMARY:-/dev/null}"
  [[ $1 == true ]] && echo "::notice title=Kolejka::$2" || echo "Pełny przebieg grupy: $2"
  exit 0
}

drzewo_commitu() { gh api "repos/${GH_REPO}/git/commits/$1" --jq .tree.sha; }

if [[ $tryb == zapisz ]]; then
  if drzewo="$(drzewo_commitu "$GITHUB_SHA")" && [[ $drzewo =~ ^[0-9a-f]{40}$ ]]; then
    mkdir -p "$DRZEWO_KATALOG"
    printf '%s\n' "$drzewo" >"$DRZEWO_KATALOG/drzewo.txt"
    echo "Drzewo commitu scalenia ${GITHUB_SHA}: ${drzewo}"
  else
    echo "::warning title=Kolejka::Nie udało się odczytać drzewa commitu scalenia; grupa kolejki wykona pełny przebieg."
  fi
  exit 0
fi

[[ $tryb == porownaj ]] || { echo "Nieznany tryb: ${tryb}" >&2; exit 2; }
[[ $GROUP_HEAD_REF =~ /pr-([0-9]+)-[0-9a-f]+$ ]] || odpowiedz false "nie rozpoznano numeru PR w gałęzi grupy."
pr="${BASH_REMATCH[1]}"
if ! drzewo_grupy="$(drzewo_commitu "$GROUP_HEAD_SHA")" || [[ ! $drzewo_grupy =~ ^[0-9a-f]{40}$ ]]; then
  odpowiedz false "nie odczytano drzewa commitu grupy."
fi
poddrzewo_github() { gh api "repos/${GH_REPO}/git/trees/$1" --jq '.tree[] | select(.path == ".github") | .sha'; }
if ! github_grupy="$(poddrzewo_github "$drzewo_grupy")" || ! github_bazy="$(poddrzewo_github "$(drzewo_commitu "$GROUP_BASE_SHA")")" ||
  [[ -z $github_grupy || $github_grupy != "$github_bazy" ]]; then
  odpowiedz false "grupa zmienia katalog .github względem bazy albo nie odczytano jego drzewa."
fi
if ! glowa="$(gh api "repos/${GH_REPO}/pulls/${pr}" --jq .head.sha)" || [[ -z $glowa ]]; then
  odpowiedz false "nie odczytano commitu PR #${pr}."
fi
if ! przebiegi="$(gh api "repos/${GH_REPO}/actions/workflows/${DRZEWO_WORKFLOW}/runs?event=pull_request&head_sha=${glowa}&per_page=100" \
  --jq '.workflow_runs[] | [.id, .status, (.conclusion // "-"), .created_at, .html_url] | @tsv')"; then
  odpowiedz false "nie odczytano przebiegów ${DRZEWO_WORKFLOW} na PR #${pr}."
fi
# Liczy się najnowszy przebieg na ostatnim commicie PR: późniejsza zmiana tytułu albo opisu (pr-guard) go zastępuje.
najnowszy="$(printf '%s\n' "$przebiegi" | sort -t "$(printf '\t')" -k4,4r | head -n 1)"
[[ -n $najnowszy ]] || odpowiedz false "brak przebiegu ${DRZEWO_WORKFLOW} na PR #${pr}."
IFS=$'\t' read -r id status wynik _ link <<<"$najnowszy"
if [[ $status != completed || $wynik != success ]]; then
  odpowiedz false "przebieg ${DRZEWO_WORKFLOW} na PR #${pr} nie jest w pełni zielony (${status}, ${wynik}): ${link}"
fi
rm -rf "$DRZEWO_KATALOG"
if ! gh run download "$id" --repo "$GH_REPO" --name drzewo-scalenia --dir "$DRZEWO_KATALOG" || [[ ! -s $DRZEWO_KATALOG/drzewo.txt ]]; then
  odpowiedz false "przebieg ${DRZEWO_WORKFLOW} na PR #${pr} nie zapisał drzewa scalenia: ${link}"
fi
drzewo_pr="$(tr -d '[:space:]' <"$DRZEWO_KATALOG/drzewo.txt")"
if [[ $drzewo_pr != "$drzewo_grupy" ]]; then
  odpowiedz false "drzewo grupy ${drzewo_grupy} różni się od sprawdzonego na PR #${pr} (${drzewo_pr}): ${link}"
fi
odpowiedz true "drzewo ${drzewo_grupy} jest równe sprawdzonemu w pełni zielonym przebiegiem ${DRZEWO_WORKFLOW} na PR #${pr}, ciężkie joby pominięte: ${link}"
