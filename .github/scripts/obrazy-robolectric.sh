#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: obrazy-robolectric.sh lista|pobierz|sprawdz.
# Robolectric pobiera obraz `android-all-instrumented` w trakcie testu jednym żądaniem, bez ponowienia, więc błąd sieci
# kończy testy modułu `initializationError`. Plik obecny w lokalnym repozytorium Maven Robolectric bierze bez sieci.
# - lista: obrazy, których używają testy, po jednym w wierszu;
# - pobierz: przed testami pobiera brakujące obrazy z ponowieniami i sprawdza sumę SHA-512;
# - sprawdz: po testach zgłasza obraz spoza listy, który Robolectric pobrał sam (nowy poziom SDK albo nowa wersja
#   Robolectric): trzeba go dopisać do listy.
# Kod wyjścia: 0 = poprawnie, 1 = błąd pobrania albo obraz spoza listy, 2 = złe użycie.
# Lustro to samo co w `dostepnykampus.android.robolectric.gradle.kts`; w teście ROBOLECTRIC_LUSTRO, ROBOLECTRIC_REPO
# i ROBOLECTRIC_PONOWIENIA (liczba ponowień curl, domyślnie 5).
lustro="${ROBOLECTRIC_LUSTRO:-https://maven-central.storage-download.googleapis.com/maven2}"
repo="${ROBOLECTRIC_REPO:-$HOME/.m2/repository}"
ponowienia="${ROBOLECTRIC_PONOWIENIA:-5}"
sciezka=org/robolectric/android-all-instrumented

# Obrazy Robolectric 4.18 (DefaultSdkProvider) dla poziomów SDK z testów: 33 (`robolectric.properties`),
# 36 (`@Config(sdk = [36])`), 37 (`androidTargetSdk`).
obrazy=(
  13-robolectric-9030017-i7
  16-robolectric-13921718-2-i7
  17-robolectric-16653203-i7
)

blad() {
  echo "::error title=Obrazy Robolectric::$1"
  exit 1
}

pobierz() {
  local obraz plik cel tymczasowy oczekiwana
  for obraz in "${obrazy[@]}"; do
    plik="android-all-instrumented-${obraz}.jar"
    cel="${repo}/${sciezka}/${obraz}"
    if [[ -s "${cel}/${plik}" ]]; then
      echo "${obraz}: już w lokalnym repozytorium"
      continue
    fi
    tymczasowy="$(mktemp -d)"
    if ! curl -fsSL --retry "$ponowienia" --retry-all-errors --retry-delay 5 -o "${tymczasowy}/${plik}" \
      "${lustro}/${sciezka}/${obraz}/${plik}" ||
      ! curl -fsSL --retry "$ponowienia" --retry-all-errors --retry-delay 5 -o "${tymczasowy}/${plik}.sha512" \
        "${lustro}/${sciezka}/${obraz}/${plik}.sha512"; then
      rm -rf "$tymczasowy"
      blad "nie pobrano ${plik} z ${lustro} po ponowieniach."
    fi
    oczekiwana="$(cut -d ' ' -f 1 < "${tymczasowy}/${plik}.sha512" | tr -d '\r\n')"
    if [[ "$(sha512sum "${tymczasowy}/${plik}" | cut -d ' ' -f 1)" != "$oczekiwana" ]]; then
      rm -rf "$tymczasowy"
      blad "${plik}: suma SHA-512 różni się od pliku .sha512 w lustrze."
    fi
    mkdir -p "$cel"
    mv "${tymczasowy}/${plik}" "${tymczasowy}/${plik}.sha512" "$cel/"
    rm -rf "$tymczasowy"
    echo "${obraz}: pobrany"
  done
}

sprawdz() {
  local katalog obraz obce=()
  shopt -s nullglob
  for katalog in "${repo}/${sciezka}"/*/; do
    obraz="$(basename "$katalog")"
    if [[ " ${obrazy[*]} " != *" ${obraz} "* ]]; then
      obce+=("$obraz")
    fi
  done
  if ((${#obce[@]} > 0)); then
    blad "Robolectric pobrał w trakcie testów obrazy spoza listy: ${obce[*]}. Dopisz je do listy w .github/scripts/obrazy-robolectric.sh."
  fi
  echo "Robolectric użył tylko obrazów z listy."
}

case "${1:-}" in
  lista) printf '%s\n' "${obrazy[@]}" ;;
  pobierz) pobierz ;;
  sprawdz) sprawdz ;;
  *)
    echo "Użycie: obrazy-robolectric.sh lista|pobierz|sprawdz" >&2
    exit 2
    ;;
esac
