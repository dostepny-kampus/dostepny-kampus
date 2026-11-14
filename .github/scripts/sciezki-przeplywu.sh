#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: sciezki-przeplywu.sh <plik workflow> [wymagana ścieżka]...
# Workflow ma dwie listy ścieżek, które decydują, czy zmiana go dotyczy: filtr `on.push.paths` i polecenie
# `zmiany-obszaru.sh <klucz> <ścieżki>` w zadaniu `zmiany` (PR i grupa kolejki). Skrypt sprawdza, że obie listy są
# takie same i że zawierają każdą wymaganą ścieżkę, czyli każdy katalog albo plik, który czytają testy tego
# workflow. `katalog/**` z filtra push i `katalog` z polecenia diff znaczą to samo.
workflow="${1:?Podaj plik workflow}"
shift

push_paths="$(awk '
  /^  push:/ { push = 1; next }
  push && /^  [a-z_]+:/ { push = 0 }
  push && /^    paths:/ { paths = 1; next }
  push && paths && /^      - / { sub(/^      - /, ""); gsub(/["\047]/, ""); sub(/\/\*\*$/, ""); print; next }
  push && paths { paths = 0 }
' "$workflow" | sort -u)"
diff_paths="$(awk '
  /^ +(- )?run: >-$/ { block = 1; text = ""; next }
  block && /^          / { text = text " " $0; next }
  block && text ~ /zmiany-obszaru\.sh/ { print text; done = 1; exit }
  { block = 0 }
  END { if (!done && block && text ~ /zmiany-obszaru\.sh/) print text }
' "$workflow" | sed -E 's/^ *[^ ]*zmiany-obszaru\.sh +[^ ]+ +//' | tr -s ' ' '\n' | sed '/^$/d' | sort -u)"

failed=0
if [[ -z $push_paths || -z $diff_paths ]]; then
  echo "::error title=Ścieżki workflow::${workflow}: nie znaleziono filtra on.push.paths albo wywołania zmiany-obszaru.sh w zadaniu zmiany."
  exit 1
fi
if [[ $push_paths != "$diff_paths" ]]; then
  echo "::error title=Ścieżki workflow::${workflow}: filtr push i zadanie zmiany mają różne ścieżki."
  echo "tylko w on.push.paths: $(comm -23 <(echo "$push_paths") <(echo "$diff_paths") | paste -sd ' ' -)"
  echo "tylko w zadaniu zmiany: $(comm -13 <(echo "$push_paths") <(echo "$diff_paths") | paste -sd ' ' -)"
  failed=1
fi
for required in "$@"; do
  if ! grep -qxF -- "$required" <<<"$diff_paths"; then
    echo "::error title=Ścieżki workflow::${workflow}: brak ścieżki ${required}, którą czytają testy tego workflow."
    failed=1
  fi
done
if ((failed == 0)); then
  echo "${workflow}: ścieżki zgodne: $(paste -sd ' ' - <<<"$diff_paths")"
fi
exit "$failed"
