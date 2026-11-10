#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test kontroli długości nagłówka squasha (naglowek-squasha.sh): „<tytuł> (#N)” ma mieć najwyżej 100 znaków, nie bajtów.
script="$(cd "$(dirname "$0")" && pwd)/naglowek-squasha.sh"
failures=0

# przypadek <nazwa> <oczekiwany kod> <fragment wyjścia> <tytuł> <numer>
przypadek() {
  local name="$1" want="$2" text="$3" status=0 out
  out="$(bash "$script" "$4" "$5" 2>&1)" || status=$?
  if ((status == want)) && [[ $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status} (oczekiwano ${want}), wyjście „${out}”, oczekiwano „${text}”"
    failures=$((failures + 1))
  fi
}

powtorz() { printf "$1%.0s" $(seq "$2"); }

przypadek "nagłówek równo 100 znaków przechodzi" 0 "ma 100 znaków" "$(powtorz a 94)" 42
przypadek "nagłówek o znak dłuższy daje błąd z długością" 1 "ma 101 znaków, limit 100; tytuł może mieć najwyżej 94 znaków (jest 95)" "$(powtorz a 95)" 42
przypadek "polskie znaki liczone jako jeden znak" 0 "ma 100 znaków" "$(powtorz ł 94)" 42
przypadek "polskie znaki: o znak za dużo daje błąd" 1 "ma 101 znaków" "$(powtorz ł 95)" 42
przypadek "dłuższy numer PR skraca dopuszczalny tytuł" 1 "najwyżej 92 znaków" "$(powtorz a 94)" 1234
przypadek "przykład z #669: 97 znaków i numer trzycyfrowy daje 104" 1 "ma 104 znaków" "$(powtorz a 97)" 669

exit $((failures > 0))
