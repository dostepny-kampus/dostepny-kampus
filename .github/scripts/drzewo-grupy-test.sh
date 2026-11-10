#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test drzewo-grupy.sh: podstawione `gh` odpowiada z plików w $fake, brak pliku oznacza błąd odczytu.
script="$(cd "$(dirname "$0")" && pwd)/drzewo-grupy.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
fake="$workdir/fake"
failures=0
mkdir "$workdir/bin"
cat >"$workdir/bin/gh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
f="$FAKE_DIR/${1}-${2##*/}"
case "$2" in
  repos/o/r/actions/workflows/*) f="$FAKE_DIR/przebiegi"; echo "$2" >"$FAKE_DIR/zapytanie" ;;
  repos/o/r/git/commits/*) f="$FAKE_DIR/drzewo-${2##*/}" ;;
  repos/o/r/git/trees/*) f="$FAKE_DIR/github-${2##*/}" ;;
  repos/o/r/pulls/*) f="$FAKE_DIR/glowa-${2##*/}" ;;
esac
[[ $1 != run ]] || { f="$FAKE_DIR/artefakt-$3"; mkdir -p "$9"; cp "$f" "$9/drzewo.txt"; exit 0; }
[[ -f $f ]] || { echo "gh: brak ${f##*/}" >&2; exit 1; }
cat "$f"
SH
chmod +x "$workdir/bin/gh"

a=$(printf 'a%.0s' {1..40})
b=$(printf 'b%.0s' {1..40})
group_sha=$(printf '1%.0s' {1..40})
base_sha=$(printf '3%.0s' {1..40})
head_sha=$(printf '2%.0s' {1..40})
t=$'\t'
green="900${t}completed${t}success${t}2026-11-10T10:00:00Z${t}https://example/runs/900"
# przypadek <nazwa> <pomin> <fragment komunikatu> <przebiegi lub BRAK> <drzewo artefaktu lub BRAK> [gałąź grupy]
przypadek() {
  local name="$1" want="$2" text="$3" runs="$4" artifact="$5" ref="${6:-refs/heads/gh-readonly-queue/sprint/4/pr-42-${group_sha}}" status=0
  rm -rf "$fake" "$workdir/out"
  mkdir "$fake"
  printf '%s\n' "$a" >"$fake/drzewo-${group_sha}"
  printf '%s\n' "$head_sha" >"$fake/glowa-42"
  printf '%s
' "$b" >"$fake/drzewo-${base_sha}"
  printf 'x
' >"$fake/github-${a}"
  printf '%s
' "${GITHUB_BAZY:-x}" >"$fake/github-${b}"
  if [[ -n ${BEZ:-} ]]; then rm "$fake/${BEZ}"; fi
  if [[ $runs != BRAK ]]; then printf '%b\n' "$runs" >"$fake/przebiegi"; fi
  if [[ $artifact != BRAK ]]; then printf '%s\n' "$artifact" >"$fake/artefakt-900"; fi
  : >"$workdir/out"
  PATH="$workdir/bin:$PATH" FAKE_DIR="$fake" GH_REPO=o/r GITHUB_OUTPUT="$workdir/out" DRZEWO_WORKFLOW=android.yml \
    GROUP_BASE_SHA="$base_sha" GROUP_HEAD_SHA="$group_sha" GROUP_HEAD_REF="$ref" DRZEWO_KATALOG="$workdir/pobrane" \
    bash "$script" porownaj >"$workdir/log" 2>&1 || status=$?
  if ((status == 0)) && [[ $(cat "$workdir/out") == "pomin=${want}" ]] && [[ $(cat "$workdir/log") == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status}, wynik „$(cat "$workdir/out")”, oczekiwano pomin=${want} i „${text}”"
    sed 's/^/    /' "$workdir/log"
    failures=$((failures + 1))
  fi
}

przypadek "drzewa równe i przebieg zielony: pomiń" true "ciężkie joby pominięte: https://example/runs/900" "$green" "$a"
[[ $(cat "$fake/zapytanie") == *"workflows/android.yml/runs?event=pull_request&head_sha=${head_sha}"* ]] ||
  { echo "BŁĄD: zapytanie o przebiegi: $(cat "$fake/zapytanie")"; failures=$((failures + 1)); }
przypadek "drzewa różne: pełny przebieg" false "różni się od sprawdzonego" "$green" "$b"
przypadek "przebieg na PR czerwony: pełny przebieg" false "nie jest w pełni zielony" "900${t}completed${t}failure${t}2026-11-10T10:00:00Z${t}u" "$a"
przypadek "przebieg na PR w toku: pełny przebieg" false "nie jest w pełni zielony" "900${t}in_progress${t}-${t}2026-11-10T10:00:00Z${t}u" "$a"
przypadek "brak przebiegu na PR: pełny przebieg" false "brak przebiegu" "" "$a"
przypadek "błąd odczytu przebiegów: pełny przebieg" false "nie odczytano przebiegów" BRAK "$a"
przypadek "przebieg bez artefaktu z drzewem: pełny przebieg" false "nie zapisał drzewa" "$green" BRAK
przypadek "gałąź grupy bez numeru PR: pełny przebieg" false "nie rozpoznano numeru PR" "$green" "$a" refs/heads/gh-readonly-queue/sprint/4/cos
przypadek "nowszy przebieg czerwony zastępuje starszy zielony: pełny przebieg" false "runs/901" \
  "${green}\n901${t}completed${t}failure${t}2026-11-10T11:00:00Z${t}https://example/runs/901" "$a"
przypadek "starszy czerwony nie zasłania nowszego zielonego: pomiń" true "runs/900" \
  "901${t}completed${t}failure${t}2026-11-10T09:00:00Z${t}https://example/runs/901\n${green}" "$a"
BEZ="drzewo-${group_sha}" przypadek "nieodczytane drzewo grupy: pełny przebieg" false "nie odczytano drzewa commitu grupy" "$green" "$a"

BEZ=glowa-42 przypadek "nieodczytana głowa PR: pełny przebieg" false "nie odczytano commitu PR" "$green" "$a"
GITHUB_BAZY=y przypadek "zmiana katalogu .github względem bazy przy równych drzewach: pełny przebieg" false "zmienia katalog .github" "$green" "$a"

# Tryb `zapisz`: drzewo commitu scalenia trafia do pliku, a błąd odczytu nie wywraca joba.
zapisz() {
  rm -rf "$workdir/zapis"
  PATH="$workdir/bin:$PATH" FAKE_DIR="$fake" GH_REPO=o/r GITHUB_SHA="$1" DRZEWO_KATALOG="$workdir/zapis" bash "$script" zapisz >"$workdir/log" 2>&1
}
printf '%s\n' "$b" >"$fake/drzewo-${head_sha}"
zapisz "$head_sha" && [[ $(cat "$workdir/zapis/drzewo.txt") == "$b" ]] && echo "OK: zapisz zapisuje drzewo commitu scalenia" ||
  { echo "BŁĄD: zapisz nie zapisał drzewa"; failures=$((failures + 1)); }
zapisz "$a" && [[ ! -e $workdir/zapis/drzewo.txt && $(cat "$workdir/log") == *"pełny przebieg"* ]] &&
  echo "OK: zapisz przy błędzie odczytu ostrzega i kończy się kodem 0" || { echo "BŁĄD: zapisz przy błędzie odczytu"; failures=$((failures + 1)); }

exit $((failures > 0))
