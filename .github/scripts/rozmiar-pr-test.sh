#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test limitu rozmiaru PR (rozmiar-pr.sh <baza> <head> [limit]) na prawdziwym, tymczasowym repozytorium git.
script="$(cd "$(dirname "$0")" && pwd)/rozmiar-pr.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
base=""

# new_repo: repozytorium z jednym commitem bazowym.
new_repo() {
  rm -rf "$workdir/repo"
  mkdir "$workdir/repo"
  cd "$workdir/repo"
  git init --quiet
  git config user.name Test
  git config user.email test@example.com
  git config core.autocrlf false
  echo baza >README.md
  git add -A
  git commit --quiet -m baza
  base="$(git rev-parse HEAD)"
}

# change <plik> <liczba linii>: nadpisuje plik podaną liczbą linii.
change() {
  mkdir -p "$(dirname "$1")"
  seq 1 "$2" >"$1"
}

commit_all() {
  git add -A
  git commit --quiet --allow-empty -m zmiana
}

# check <nazwa> <oczekiwany kod> <fragment wyniku> [limit]
check() {
  local name="$1" want="$2" text="$3" limit="${4:-}" out status=0
  if [[ -n $limit ]]; then
    out="$(bash "$script" "$base" HEAD "$limit" 2>&1)" || status=$?
  else
    out="$(bash "$script" "$base" HEAD 2>&1)" || status=$?
  fi
  if ((status == want)) && [[ $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status} (oczekiwano ${want}), szukano „${text}” w: ${out//$'\n'/ | }"
    failures=$((failures + 1))
  fi
}

new_repo
change src/a.txt 300
commit_all
check "300 linii przy domyślnym limicie 300 przechodzi" 0 "Rozmiar PR: 300 zmienionych linii, limit 300."

new_repo
change src/a.txt 301
commit_all
check "301 linii przy domyślnym limicie 300 nie przechodzi" 1 "301 zmienionych linii, limit 300"
check "komunikat wskazuje największy plik" 1 "src/a.txt"

new_repo
change src/a.txt 50
commit_all
check "limit z trzeciego argumentu: równy wynikowi przechodzi" 0 "limit 50" 50
check "limit z trzeciego argumentu: o jeden mniejszy nie przechodzi" 1 "50 zmienionych linii, limit 49" 49

new_repo
change src/a.txt 200
git add -A
git commit --quiet -m dodanie
base="$(git rev-parse HEAD)"
seq 201 400 >src/a.txt
commit_all
check "liczą się linie dodane i usunięte razem" 1 "400 zmienionych linii"

new_repo
change src/a.txt 10
change api/package-lock.json 1000
change data/buildings/a.geojson 1000
change data/buildings/a.json 1000
change docs/docs/_generated/a.md 1000
change android/core/generated/A.kt 1000
change android/gradle.lockfile 1000
change backend/deps.lock 1000
change android/gradlew 1000
change android/gradle/wrapper/gradle-wrapper.properties 1000
commit_all
check "dane, pliki lock i kod generowany nie liczą się do limitu" 0 "Rozmiar PR: 10 zmienionych linii"

new_repo
change src/a.txt 5
printf '\0\0\0\0' >src/obraz.bin
commit_all
check "plik binarny liczy się jako zero linii" 0 "Rozmiar PR: 5 zmienionych linii"

new_repo
commit_all
check "zmiana bez żadnych linii daje zero" 0 "Rozmiar PR: 0 zmienionych linii"

exit "$((failures > 0 ? 1 : 0))"
