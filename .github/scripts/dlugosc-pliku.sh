#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
# Sprawdza, że plik ($1) ma najwyżej $2 linii. Kod 0 = mieści się, 1 = za długi, 2 = brak pliku albo zły limit.
set -euo pipefail

file="$1"
limit="$2"

if [[ ! -f $file ]]; then
  echo "::error title=Długość pliku::Brak pliku ${file}."
  exit 2
fi
if [[ ! $limit =~ ^[1-9][0-9]*$ ]]; then
  echo "::error title=Długość pliku::Limit musi być dodatnią liczbą całkowitą, jest: ${limit}."
  exit 2
fi

# Ostatnia linia bez znaku nowej linii też się liczy.
count="$(awk 'END { print NR }' "$file")"
if ((count > limit)); then
  echo "::error file=${file},title=Długość pliku::${file} ma ${count} linii, limit to ${limit}. Skróć plik albo przenieś szczegóły do dokumentacji modułu."
  exit 1
fi
echo "${file}: ${count} z ${limit} linii."
