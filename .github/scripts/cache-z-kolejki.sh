#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: cache-z-kolejki.sh <SHA commita> <nazwy artefaktów> <katalog docelowy> [SHA sprzed pusha]
# Po scaleniu commit na gałęzi jest tym samym commitem, który sprawdziła grupa kolejki. Skrypt pobiera artefakt
# z udanego przebiegu `android` tej grupy (zdarzenie merge_group, ten sam SHA), nigdy z przebiegu pull_request.
# Kilka nazw rozdzielonych spacją (cache budowania wysyła kilka zadań grupy) trafia do jednego katalogu; przebieg
# się liczy, gdy ma choć jeden z nich. Wynik w GITHUB_OUTPUT: jest=true, gdy artefakt pobrano. Brak artefaktu (grupa pominęła budowanie, bo drzewo było
# sprawdzone na PR) albo błąd nie jest porażką: cache gałęzi zostaje bez zmian.
# Kolejka potrafi scalić kilka PR jednym pushem; wtedy artefakt zostawiła grupa wcześniejszego commita. Z czwartym
# argumentem skrypt sprawdza grupy wszystkich commitów pusha, od najnowszego.
# Środowisko: GH_TOKEN, GH_REPO; CACHE_WORKFLOW (domyślnie android.yml).
sha="${1:?Podaj SHA commita}"
read -ra names <<<"${2:?Podaj nazwy artefaktów}"
dest="${3:?Podaj katalog docelowy}"
output="${GITHUB_OUTPUT:-/dev/null}"

# pobierz <przebieg>: wszystkie artefakty z names, które ten przebieg ma, scalone w dest; kod 1, gdy żadnego.
pobierz() {
  local part="${dest}.czesc" name found=1
  rm -rf "$dest"
  for name in "${names[@]}"; do
    rm -rf "$part"
    if gh run download "$1" --name "$name" --dir "$part" && [[ -n $(ls -A "$part") ]]; then
      if [[ -d $dest ]]; then cp -a "$part/." "$dest/"; else mv "$part" "$dest"; fi
      found=0
    fi
  done
  rm -rf "$part"
  return "$found"
}

before="${4:-}"
commits="$sha"
if [[ -n $before && ! $before =~ ^0+$ ]]; then
  # Błąd odczytu zakresu nie jest porażką: zostaje sam commit głowy.
  commits="$(gh api "repos/${GH_REPO}/compare/${before}...${sha}" --jq '[.commits[].sha] | reverse | .[]')" || commits="$sha"
  [[ -n $commits ]] || commits="$sha"
fi

for commit in $commits; do
  if ! runs="$(gh run list --workflow "${CACHE_WORKFLOW:-android.yml}" --event merge_group --commit "$commit" --status success \
    --limit 5 --json databaseId --jq '.[].databaseId')"; then
    echo "::warning title=Cache z kolejki::Nie udało się odczytać przebiegów grupy kolejki dla ${commit}."
    continue
  fi
  for run in $runs; do
    if pobierz "$run"; then
      echo "Artefakty ${names[*]} z przebiegu ${run} grupy kolejki commita ${commit}: $(find "$dest" -type f | wc -l) plików."
      echo "jest=true" >>"$output"
      exit 0
    fi
  done
done
echo "::notice title=Cache z kolejki::Żadna grupa kolejki commitów tego pusha nie zostawiła artefaktu ${names[*]}; cache gałęzi bez zmian."
echo "jest=false" >>"$output"
