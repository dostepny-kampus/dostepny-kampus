#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

header=(
  "Dostępny Kampus"
  "Author: Vladyslav Yevkov"
  "Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko"
)

# Wyjątki: pliki z zakresu, których nie piszemy sami, więc nie dostają nagłówka.
excluded=(
  # Katalogi generowane przy budowaniu i instalacji zależności (zwykle nieśledzone, ale na wszelki wypadek).
  ':(glob,exclude)**/build/**'
  ':(glob,exclude)**/target/**'
  ':(glob,exclude)**/node_modules/**'
  ':(glob,exclude)**/.gradle/**'
  # Wrappery Gradle i Mavena: pliki generuje narzędzie (np. MavenWrapperDownloader.java), aktualizuje się je w całości.
  ':(glob,exclude)**/gradle/wrapper/**'
  ':(glob,exclude)**/.mvn/wrapper/**'
)

# Oczekiwane pierwsze linie pliku według języka; ustawia expected, after_shebang i form.
set_form() {
  local prefix="# " line
  expected=()
  after_shebang=""
  case "$1" in
    *.kt | *.kts | *.java)
      form="blok /* … */ na początku pliku"
      expected=("/*")
      for line in "${header[@]}"; do
        expected+=(" * $line")
      done
      expected+=(" */")
      return
      ;;
    *.sql)
      form="linie -- … na początku pliku"
      prefix="-- "
      ;;
    *.py | *.sh)
      form="linie # … na początku pliku, pod shebangiem i deklaracją kodowania"
      after_shebang="tak"
      ;;
  esac
  for line in "${header[@]}"; do
    expected+=("${prefix}${line}")
  done
}

has_header() {
  local path="$1" start=0 i
  local -a lines=()
  set_form "$path"
  mapfile -t -n $((${#expected[@]} + 2)) lines <"$path"
  for i in "${!lines[@]}"; do
    lines[i]="${lines[i]%$'\r'}"
  done
  if [[ -n "$after_shebang" ]]; then
    if [[ "${lines[start]:-}" == '#!'* ]]; then
      start=$((start + 1))
    fi
    if [[ "${lines[start]:-}" =~ ^#.*coding[:=] ]]; then
      start=$((start + 1))
    fi
  fi
  for i in "${!expected[@]}"; do
    if [[ "${lines[start + i]:-}" != "${expected[i]}" ]]; then
      return 1
    fi
  done
}

found=0
checked=0
while IFS= read -r -d '' path; do
  checked=$((checked + 1))
  if ! has_header "$path"; then
    echo "::error file=${path},line=1::Brak nagłówka autorskiego albo nagłówek niezgodny ze wzorcem (${form})."
    found=1
  fi
done < <(git ls-files -z -- '*.kt' '*.kts' '*.java' '*.py' '*.sh' '*.sql' "${excluded[@]}")

if ((found == 0)); then
  echo "Nagłówek autorski jest we wszystkich ${checked} plikach z kodem (.kt, .kts, .java, .py, .sh, .sql)."
fi
exit "$found"
