#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test przytnij-cache-budowania.sh: trzy wpisy z cache, jeden odczytany przez build (czas dostępu ustawiony ręcznie,
# bo montowanie systemu testowego może pomijać czas dostępu), jeden zapisany przez build; nieużyty wpis znika.
script="$(cd "$(dirname "$0")" && pwd)/przytnij-cache-budowania.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
sprawdz() {
  if eval "$2"; then echo "OK: $1"; else echo "BŁĄD: $1"; failures=$((failures + 1)); fi
}

cache="$workdir/build-cache-1"
mkdir "$cache"
echo a >"$cache/uzyty"
echo b >"$cache/nieuzyty"
bash "$script" postarz "$cache" >/dev/null
touch -m -d '2001-01-01 00:00:00 UTC' "$workdir/znacznik"
sprawdz "postarz ustawia stary czas dostępu" '[[ -z $(find "$cache" -type f -anewer "$workdir/znacznik") ]]'
touch -a "$cache/uzyty"
echo c >"$cache/nowy"
bash "$script" przytnij "$cache" >"$workdir/log"
sprawdz "przytnij zostawia odczytany wpis" '[[ -f $cache/uzyty ]]'
sprawdz "przytnij zostawia nowy wpis" '[[ -f $cache/nowy ]]'
sprawdz "przytnij usuwa nieużyty wpis" '[[ ! -e $cache/nieuzyty ]]'
sprawdz "przytnij podaje liczbę usuniętych" 'grep -q "usunięto 1 " "$workdir/log"'
sprawdz "brak katalogu nie jest błędem" 'bash "$script" przytnij "$workdir/brak" >/dev/null'
status=0
bash "$script" zly "$cache" >/dev/null 2>&1 || status=$?
sprawdz "nieznany tryb to błąd użycia" '((status == 2))'

exit "$((failures > 0 ? 1 : 0))"
