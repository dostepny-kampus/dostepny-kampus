#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test pobierania kilku artefaktów przebiegu do jednego katalogu (pobierz-artefakty.sh). Podstawione `gh` pobiera
# artefakt <nazwa> przebiegu <id> tylko wtedy, gdy jest plik `<id>-<nazwa>`, i zapisuje go w katalogu jako <nazwa>.
script="$(cd "$(dirname "$0")" && pwd)/pobierz-artefakty.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
mkdir "$workdir/bin" "$workdir/f"
cat >"$workdir/bin/gh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
[[ "$1 $2 $4 $6" == 'run download --name --dir' && -f $FAKE_DIR/$3-$5 ]] || exit 1
mkdir -p "$7"
cp "$FAKE_DIR/$3-$5" "$7/$5"
SH
chmod +x "$workdir/bin/gh"

# przypadek <nazwa> <oczekiwany kod> <oczekiwana zawartość katalogu> <artefakty przebiegu 9>
przypadek() {
  local name="$1" want_status="$2" want="$3" status=0 got artifact
  rm -rf "$workdir/f"/* "$workdir/cache" "$workdir/cache.czesc"
  for artifact in $4; do echo "$artifact" >"$workdir/f/9-$artifact"; done
  mkdir "$workdir/cache" && echo stary >"$workdir/cache/stary"
  PATH="$workdir/bin:$PATH" FAKE_DIR="$workdir/f" bash "$script" 9 "$workdir/cache" a b >/dev/null 2>&1 || status=$?
  got=""
  [[ -d $workdir/cache ]] && got="$(ls -A "$workdir/cache" | tr '\n' ' ')"
  if ((status == want_status)) && [[ $got == "$want" && ! -e $workdir/cache.czesc ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status}, katalog „${got}”, oczekiwano „${want}”"
    failures=$((failures + 1))
  fi
}

przypadek "oba artefakty scalone w jednym katalogu" 0 "a b " "a b"
przypadek "wystarczy jeden artefakt" 0 "b " "b"
przypadek "brak artefaktów: kod 1, pusty katalog" 1 "" ""
status=0
bash "$script" 9 "$workdir/cache" >/dev/null 2>&1 || status=$?
((status == 2)) && echo "OK: bez nazw artefaktów kod 2" || { echo "BŁĄD: bez nazw: kod ${status}"; failures=$((failures + 1)); }

exit $((failures > 0))
