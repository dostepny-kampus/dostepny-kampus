#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/obrazy-robolectric.sh"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
cd "$test_dir"
sciezka=org/robolectric/android-all-instrumented
mapfile -t obrazy < <(bash "$script" lista)

# Lustro z plikami jar i .sha512 dla każdego obrazu z listy skryptu.
przygotuj_lustro() {
  rm -rf lustro repo
  local obraz katalog
  for obraz in "${obrazy[@]}"; do
    katalog="lustro/$sciezka/$obraz"
    mkdir -p "$katalog"
    printf 'obraz %s\n' "$obraz" > "$katalog/android-all-instrumented-$obraz.jar"
    sha512sum "$katalog/android-all-instrumented-$obraz.jar" | cut -d ' ' -f 1 > "$katalog/android-all-instrumented-$obraz.jar.sha512"
  done
}

run_case() {
  local name="$1" expected="$2" status=0
  shift 2
  ROBOLECTRIC_LUSTRO="file://$test_dir/lustro" ROBOLECTRIC_REPO="$test_dir/repo" ROBOLECTRIC_PONOWIENIA=0 \
    bash "$script" "$@" > output 2>&1 || status=$?
  if [[ $status != "$expected" ]]; then
    echo "BŁĄD: $name: kod $status zamiast $expected"
    cat output
    exit 1
  fi
}

if ((${#obrazy[@]} == 0)); then
  echo "BŁĄD: skrypt nie podaje żadnego obrazu"
  exit 1
fi
echo "OK: lista ma ${#obrazy[@]} obrazy"

przygotuj_lustro
run_case "pobierz: wszystkie obrazy z sumą kontrolną" 0 pobierz
for obraz in "${obrazy[@]}"; do
  cmp -s "lustro/$sciezka/$obraz/android-all-instrumented-$obraz.jar" "repo/$sciezka/$obraz/android-all-instrumented-$obraz.jar"
  test -s "repo/$sciezka/$obraz/android-all-instrumented-$obraz.jar.sha512"
done
echo "OK: pobierz kopiuje jar i .sha512 do lokalnego repozytorium Maven"

rm -rf lustro
run_case "pobierz: obraz już jest, bez sieci" 0 pobierz
echo "OK: obraz w lokalnym repozytorium nie jest pobierany drugi raz"

przygotuj_lustro
printf 'inny\n' > "lustro/$sciezka/${obrazy[0]}/android-all-instrumented-${obrazy[0]}.jar"
run_case "pobierz: zła suma kontrolna" 1 pobierz
grep -q '::error title=Obrazy Robolectric::' output
if [[ -e "repo/$sciezka/${obrazy[0]}/android-all-instrumented-${obrazy[0]}.jar" ]]; then
  echo "BŁĄD: jar ze złą sumą kontrolną trafił do repozytorium"
  exit 1
fi
echo "OK: jar ze złą sumą kontrolną odrzucony"

przygotuj_lustro
rm -rf "lustro/$sciezka/${obrazy[0]}"
run_case "pobierz: brak obrazu w lustrze" 1 pobierz
grep -q '::error title=Obrazy Robolectric::' output
echo "OK: brak obrazu w lustrze to błąd z nazwą obrazu"

przygotuj_lustro
run_case "sprawdź: tylko obrazy z listy" 0 pobierz
run_case "sprawdź: tylko obrazy z listy" 0 sprawdz
echo "OK: sprawdz przechodzi, gdy Robolectric użył tylko obrazów z listy"

mkdir -p "repo/$sciezka/99-robolectric-1-i7"
run_case "sprawdź: obraz spoza listy" 1 sprawdz
grep -q '99-robolectric-1-i7' output
echo "OK: sprawdz zgłasza obraz, który Robolectric pobrał sam"

run_case "nieznane polecenie" 2 inne
echo "OK: nieznane polecenie kończy się kodem 2"
