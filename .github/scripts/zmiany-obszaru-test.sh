#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test zmiany-obszaru.sh na repozytorium z trzema commitami: baza, zmiana w app/, zmiana w docs/. PR to ostatni
# commit; grupa w kolejce to zakres od bazy do ostatniego commitu, pobierany z repozytorium „origin”.
script="$(cd "$(dirname "$0")" && pwd)/zmiany-obszaru.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0

origin="$workdir/origin"
git init -q "$origin"
git -C "$origin" config uploadpack.allowAnySHA1InWant true
commit() {
  mkdir -p "$origin/$(dirname "$1")"
  echo "$2" >"$origin/$1"
  git -C "$origin" add -A
  git -C "$origin" -c user.name=t -c user.email=t@t commit -qm "$2"
  git -C "$origin" rev-parse HEAD
}
base="$(commit README.md baza)"
commit app/main.kt aplikacja >/dev/null
head="$(commit docs/strona.md dokumentacja)"
clone="$workdir/klon"
git clone -q --depth=2 "file://$origin" "$clone"

# przypadek <nazwa> <oczekiwany wynik> <zdarzenie> <klucz i ścieżki…>
przypadek() {
  local name="$1" want="$2" event="$3" status=0 out
  shift 3
  : >"$workdir/output"
  out="$(cd "$clone" && EVENT="$event" GROUP_BASE_SHA="$base" GROUP_HEAD_SHA="$head" \
    GITHUB_OUTPUT="$workdir/output" bash "$script" "$@" 2>&1)" || status=$?
  if ((status == 0)) && [[ $(cat "$workdir/output") == "$want" ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status}, wynik „$(cat "$workdir/output")”, oczekiwano „${want}”"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

przypadek "PR zmienia obszar" "dok=true" pull_request dok docs
przypadek "PR nie zmienia obszaru" "app=false" pull_request app app
przypadek "PR zmienia plik wskazany wzorcem glob" "dok=true" pull_request dok ':(glob)docs/**/*.md'
przypadek "grupa w kolejce obejmuje wszystkie commity od bazy" "app=true" merge_group app app
przypadek "grupa w kolejce bez zmian obszaru" "inne=false" merge_group inne inne README.md

status=0
(cd "$clone" && GITHUB_OUTPUT="$workdir/output" bash "$script" klucz >/dev/null 2>&1) || status=$?
if ((status == 2)); then
  echo "OK: brak ścieżek to błąd użycia"
else
  echo "BŁĄD: brak ścieżek: kod ${status}, oczekiwano 2"
  failures=$((failures + 1))
fi

exit "$((failures > 0 ? 1 : 0))"
