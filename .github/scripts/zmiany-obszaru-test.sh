#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test zmiany-obszaru.sh na repozytorium z trzema commitami: baza, zmiana w app/, zmiana w docs/. Ostatni commit to PR
# albo pozycja kolejki stojąca za pozycją ze zmianą app/.
script="$(cd "$(dirname "$0")" && pwd)/zmiany-obszaru.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0

origin="$workdir/origin"
git init -q "$origin"
commit() {
  mkdir -p "$origin/$(dirname "$1")"
  echo "$2" >"$origin/$1"
  git -C "$origin" add -A
  git -C "$origin" -c user.name=t -c user.email=t@t commit -qm "$2"
  git -C "$origin" rev-parse HEAD
}
commit README.md baza >/dev/null
commit app/main.kt aplikacja >/dev/null
commit docs/strona.md dokumentacja >/dev/null
clone="$workdir/klon"
git clone -q --depth=2 "file://$origin" "$clone"

# przypadek <nazwa> <oczekiwany wynik> <klucz i ścieżki…>
przypadek() {
  local name="$1" want="$2" status=0 out
  shift 2
  : >"$workdir/output"
  out="$(cd "$clone" && GITHUB_OUTPUT="$workdir/output" bash "$script" "$@" 2>&1)" || status=$?
  if ((status == 0)) && [[ $(cat "$workdir/output") == "$want" ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status}, wynik „$(cat "$workdir/output")”, oczekiwano „${want}”"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

przypadek "ostatni commit zmienia obszar" "dok=true" dok docs
przypadek "ostatni commit nie zmienia obszaru" "app=false" app app
przypadek "ostatni commit zmienia plik wskazany wzorcem glob" "dok=true" dok ':(glob)docs/**/*.md'
przypadek "pozycja kolejki za zmianą app/ nie dziedziczy jej obszaru" "app=false" app app README.md

status=0
(cd "$clone" && GITHUB_OUTPUT="$workdir/output" bash "$script" klucz >/dev/null 2>&1) || status=$?
if ((status == 2)); then
  echo "OK: brak ścieżek to błąd użycia"
else
  echo "BŁĄD: brak ścieżek: kod ${status}, oczekiwano 2"
  failures=$((failures + 1))
fi

exit "$((failures > 0 ? 1 : 0))"
