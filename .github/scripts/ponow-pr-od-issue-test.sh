#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test ponow-pr-od-issue.sh: podstawione `gh` czyta otwarte PR z $FAKE_DIR/prs, opis PR N z $FAKE_DIR/body-N,
# nieudany przebieg kontroli na głowie PR N z $FAKE_DIR/run-N (już po filtrze --jq) i zapisuje ponowione
# przebiegi w $FAKE_DIR/rerun; plik $FAKE_DIR/rerun-blad każe ponowieniu skończyć się błędem.
script="$(cd "$(dirname "$0")" && pwd)/ponow-pr-od-issue.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
fake="$workdir/fake"
failures=0
mkdir "$workdir/bin"
cat >"$workdir/bin/gh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
case "$1 $2" in
  "pr list") cat "$FAKE_DIR/prs" ;;
  "pr view")
    if [[ $* == *headRefOid* ]]; then echo "glowa-$3"; else cat "$FAKE_DIR/body-$3" 2>/dev/null || true; fi ;;
  "run list")
    while (($#)) && [[ $1 != --commit ]]; do shift; done
    cat "$FAKE_DIR/run-${2#glowa-}" 2>/dev/null || true ;;
  "run rerun")
    [[ -f $FAKE_DIR/rerun-blad ]] && exit 1
    echo "$3 $4" >>"$FAKE_DIR/rerun" ;;
  *) echo "nieznane wywołanie: $*" >&2; exit 2 ;;
esac
SH
chmod +x "$workdir/bin/gh"

# opis <PR> <tekst>, przebieg <PR> <identyfikator>
opis() { printf '%s' "$2" >"$fake/body-$1"; }
przebieg() { printf '%s\n' "$2" >"$fake/run-$1"; }

# sprawdz <nazwa> <oczekiwane ponowienia, po jednym w wierszu> <fragment wyniku>
sprawdz() {
  local name="$1" want="$2" text="$3" status=0 out got
  out="$(PATH="$workdir/bin:$PATH" FAKE_DIR="$fake" bash "$script" 42 2>&1)" || status=$?
  got="$(cat "$fake/rerun" 2>/dev/null || true)"
  if ((status == 0)) && [[ $got == "$want" && $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status}, ponowione „${got}”, oczekiwano „${want}” i „${text}”"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

nowy() {
  rm -rf "$fake"
  mkdir "$fake"
  printf '%s\n' "$@" >"$fake/prs"
}

nowy 7 8 9
opis 7 $'Część #1\n\nCloses #42'
przebieg 7 1001
opis 8 'Closes #420'
przebieg 8 1002
opis 9 'fixes #42 i resolves #43'
przebieg 9 1003
sprawdz "ponawia tylko PR zamykające issue" $'1001 --failed\n1003 --failed' "PR #9: ponowiono"

nowy 7
opis 7 'Closes #42'
sprawdz "przebieg udany albo trwający zostaje" "" "nie jest nieudany"

nowy 7
opis 7 'Część #42'
przebieg 7 1001
sprawdz "wzmianka bez słowa zamykającego nie wystarcza" "" ""

nowy 7
opis 7 'Closes #42'
przebieg 7 1001
: >"$fake/rerun-blad"
sprawdz "błąd ponowienia: ostrzeżenie i kod 0" "" "nie udało się ponowić"

nowy ''
sprawdz "brak otwartych PR" "" ""

exit "$((failures > 0 ? 1 : 0))"
