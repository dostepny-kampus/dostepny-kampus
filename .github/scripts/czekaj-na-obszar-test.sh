#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test czekaj-na-obszar.sh: podstawione `gh` zwraca przy n-tym odczycie etykiety z pliku $FAKE_DIR/n, a liczbę
# odczytów zapisuje w $FAKE_DIR/licznik.
script="$(cd "$(dirname "$0")" && pwd)/czekaj-na-obszar.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
fake="$workdir/fake"
failures=0
mkdir "$workdir/bin"
cat >"$workdir/bin/gh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
n=$(($(cat "$FAKE_DIR/licznik" 2>/dev/null || echo 0) + 1))
echo "$n" >"$FAKE_DIR/licznik"
[[ -f $FAKE_DIR/blad ]] && exit 1
cat "$FAKE_DIR/$n" 2>/dev/null || true
SH
chmod +x "$workdir/bin/gh"

# przypadek <nazwa> <oczekiwana liczba odczytów> <fragment wyniku> <etykiety kolejnych odczytów…>
przypadek() {
  local name="$1" reads="$2" text="$3" status=0 out i=1
  shift 3
  rm -rf "$fake"
  mkdir "$fake"
  for labels in "$@"; do
    if [[ $labels == BLAD ]]; then : >"$fake/blad"; else printf '%b' "$labels" >"$fake/$i"; fi
    i=$((i + 1))
  done
  out="$(PATH="$workdir/bin:$PATH" FAKE_DIR="$fake" CZEKAJ_PROBY=4 CZEKAJ_SEKUNDY=0 bash "$script" 42 2>&1)" || status=$?
  if ((status == 0)) && [[ $(cat "$fake/licznik") == "$reads" && $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status}, odczyty $(cat "$fake/licznik" 2>/dev/null), oczekiwano ${reads} i „${text}”"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

przypadek "etykieta obszaru od razu: bez czekania" 1 "ma etykietę obszaru ci" 'ci\n'
przypadek "etykieta obszaru przy trzecim odczycie: czeka na nią" 3 "ma etykietę obszaru android" '' 'bug\n' 'bug\nandroid\n'
przypadek "same etykiety typu: kończy po limicie odczytów" 4 "nadal bez etykiety obszaru" 'bug\n' 'bug\n' 'bug\n' 'bug\n'
przypadek "etykieta wielowyrazowa nie jest obszarem" 4 "nadal bez etykiety obszaru" 'good first issue\n'
przypadek "błąd gh: kończy po limicie z kodem 0" 4 "nadal bez etykiety obszaru" BLAD

exit "$((failures > 0 ? 1 : 0))"
