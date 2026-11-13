#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test pobierania artefaktu z grupy kolejki tego samego commita (cache-z-kolejki.sh). Podstawione `gh`: lista
# przebiegów z pliku `lista`, pobranie działa tylko dla przebiegów z plikiem `artefakt-<id>`; wywołania w `wywolania`.
script="$(cd "$(dirname "$0")" && pwd)/cache-z-kolejki.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
mkdir "$workdir/bin"
cat >"$workdir/bin/gh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
echo "$*" >>"$FAKE_DIR/wywolania"
case "$1 $2" in
  'run list') [[ -f $FAKE_DIR/lista ]] || exit 1; cat "$FAKE_DIR/lista" ;;
  'run download')
    [[ -f $FAKE_DIR/artefakt-$3 ]] || exit 1
    mkdir -p "$7"; cp "$FAKE_DIR/artefakt-$3" "$7/wpis" ;;
esac
SH
chmod +x "$workdir/bin/gh"

# przypadek <nazwa> <oczekiwany plik lub BRAK> <oczekiwane jest=> <fragment wyjścia> <przebiegi> <przebiegi z artefaktem>
przypadek() {
  local name="$1" want="$2" flag="$3" text="$4" runs="$5" with="$6" out status=0 id got
  rm -rf "$workdir/f" "$workdir/cache" "$workdir/output"
  mkdir "$workdir/f"
  if [[ $runs != BRAK ]]; then printf '%s\n' $runs >"$workdir/f/lista"; fi
  for id in $with; do echo "wpis-$id" >"$workdir/f/artefakt-$id"; done
  out="$(PATH="$workdir/bin:$PATH" FAKE_DIR="$workdir/f" GH_REPO=o/r GITHUB_OUTPUT="$workdir/output" \
    bash "$script" abc123 gradle-build-cache "$workdir/cache" 2>&1)" || status=$?
  got="BRAK"
  [[ -f $workdir/cache/wpis ]] && got="$(cat "$workdir/cache/wpis")"
  if ((status == 0)) && [[ $got == "$want" && $out == *"$text"* && $(cat "$workdir/output") == "jest=${flag}" ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status}, plik „${got}”, wynik „$(cat "$workdir/output" 2>/dev/null)”: ${out}"
    failures=$((failures + 1))
  fi
}

przypadek "bierze artefakt z grupy kolejki commita" wpis-9 true "przebiegu 9" "9" "9"
przypadek "pomija przebieg bez artefaktu" wpis-8 true "przebiegu 8" "9 8" "8"
przypadek "grupa pominęła budowanie: brak artefaktu, kod 0" BRAK false "nie zostawiła artefaktu" "9" ""
przypadek "commit bez grupy kolejki: kod 0" BRAK false "nie zostawiła artefaktu" "" ""
przypadek "błąd listy przebiegów: kod 0" BRAK false "Nie udało się odczytać" BRAK ""
przypadek "zapytanie o przebiegi" wpis-9 true "przebiegu 9" "9" "9"
grep -q -- '--event merge_group' "$workdir/f/wywolania" && grep -q -- '--commit abc123' "$workdir/f/wywolania" &&
  grep -q -- '--status success' "$workdir/f/wywolania" && grep -q -- '--name gradle-build-cache' "$workdir/f/wywolania" &&
  echo "OK: zapytanie ogranicza zdarzenie merge_group, commit, status success i nazwę artefaktu" ||
  { echo "BŁĄD: zapytanie o przebiegi: $(cat "$workdir/f/wywolania")"; failures=$((failures + 1)); }

exit $((failures > 0))
