#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: pobierz-artefakty.sh <przebieg> <katalog docelowy> <nazwa artefaktu>...
# Pobiera z przebiegu każdy z podanych artefaktów, który przebieg ma, i scala je w jednym katalogu (części zadania
# z macierzą wysyłają cache budowania osobno; wpisy cache są nazwane skrótem treści, więc scalenie ich nie psuje).
# Poprzednia zawartość katalogu znika. Kod 1, gdy przebieg nie ma żadnego z artefaktów. Środowisko: GH_TOKEN, GH_REPO.
run="${1:?Podaj przebieg}"
dest="${2:?Podaj katalog docelowy}"
shift 2
(($# > 0)) || { echo "Podaj co najmniej jedną nazwę artefaktu." >&2; exit 2; }
part="${dest}.czesc"
found=1
rm -rf "$dest"
for name in "$@"; do
  rm -rf "$part"
  if gh run download "$run" --name "$name" --dir "$part" && [[ -n $(ls -A "$part") ]]; then
    if [[ -d $dest ]]; then cp -a "$part/." "$dest/"; else mv "$part" "$dest"; fi
    found=0
  fi
done
rm -rf "$part"
exit "$found"
