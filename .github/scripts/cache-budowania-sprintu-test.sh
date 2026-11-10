#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test pobierania cache budowania z przebiegu sprintu (cache-budowania-sprintu.sh). Podstawione `gh`: lista przebiegów
# z pliku `lista`, pobranie działa tylko dla przebiegów z plikiem `artefakt-<id>`; wywołania zapisuje do `wywolania`.
script="$(cd "$(dirname "$0")" && pwd)/cache-budowania-sprintu.sh"
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

# przypadek <nazwa> <oczekiwany plik w katalogu lub BRAK> <fragment wyjścia> <przebiegi> <przebiegi z artefaktem>
przypadek() {
  local name="$1" want="$2" text="$3" runs="$4" with="$5" out status=0 id
  rm -rf "$workdir/f" "$workdir/cache"
  mkdir "$workdir/f"
  if [[ $runs != BRAK ]]; then printf '%s\n' $runs >"$workdir/f/lista"; fi
  for id in $with; do echo "wpis-$id" >"$workdir/f/artefakt-$id"; done
  out="$(PATH="$workdir/bin:$PATH" FAKE_DIR="$workdir/f" GH_REPO=o/r GITHUB_RUN_ID=3 bash "$script" sprint/4 "$workdir/cache" 2>&1)" || status=$?
  got="BRAK"
  [[ -f $workdir/cache/wpis ]] && got="$(cat "$workdir/cache/wpis")"
  if ((status == 0)) && [[ $got == "$want" && $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status}, plik „${got}”, oczekiwano „${want}” i „${text}”: ${out}"
    failures=$((failures + 1))
  fi
}

przypadek "bierze najnowszy przebieg z artefaktem" wpis-9 "przebiegu 9" "9 8" "9 8"
przypadek "pomija przebieg bez artefaktu (wygasł)" wpis-8 "przebiegu 8" "9 8" "8"
przypadek "pomija własny przebieg" wpis-8 "przebiegu 8" "3 8" "3 8"
przypadek "brak artefaktów: zimny cache, kod 0" BRAK "Brak artefaktu cache" "9 8" ""
przypadek "błąd listy przebiegów: zimny cache, kod 0" BRAK "Nie udało się odczytać" BRAK ""
przypadek "lista tylko udanych przebiegów push na gałęzi sprintu" wpis-9 "przebiegu 9" "9" "9"
grep -q -- '--event push' "$workdir/f/wywolania" && grep -q -- '--status success' "$workdir/f/wywolania" && grep -q -- '--branch sprint/4' "$workdir/f/wywolania" &&
  echo "OK: zapytanie ogranicza gałąź, zdarzenie push i status success" ||
  { echo "BŁĄD: zapytanie o przebiegi: $(cat "$workdir/f/wywolania")"; failures=$((failures + 1)); }

exit $((failures > 0))
