#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: przytnij-cache-budowania.sh postarz|przytnij <katalog build-cache Gradle>
# Cache budowania przechodzi z grupy do grupy kolejki i bez przycinania rośnie o wyniki starych commitów.
# postarz (po odtworzeniu cache, przed Gradle): ustawia czas dostępu wszystkich wpisów na 2000-01-01. Przy montowaniu
# relatime pierwszy odczyt pliku z czasem dostępu starszym niż modyfikacja ustawia czas dostępu na bieżący.
# przytnij (po Gradle, przed wysłaniem cache): usuwa wpisy, których ten build nie odczytał ani nie zapisał.
# Montowanie noatime nie aktualizuje czasu dostępu: wtedy oba tryby niczego nie zmieniają, z ostrzeżeniem.
tryb="${1:?Podaj tryb: postarz albo przytnij}"
katalog="${2:?Podaj katalog build-cache}"
stary='2000-01-01 00:00:00 UTC'

[[ -d $katalog ]] || { echo "Brak katalogu ${katalog}: nic do zrobienia."; exit 0; }
if [[ ,$(findmnt -n -o OPTIONS -T "$katalog" 2>/dev/null || true), == *,noatime,* ]]; then
  echo "::warning title=Cache budowania::System plików bez czasu dostępu (noatime); cache bez przycinania."
  exit 0
fi

rozmiar() { du -sm "$katalog" | cut -f1; }
case "$tryb" in
  postarz)
    find "$katalog" -type f -exec touch -a -d "$stary" {} +
    echo "Cache budowania: $(find "$katalog" -type f | wc -l) wpisów, $(rozmiar) MB, postarzone przed budowaniem."
    ;;
  przytnij)
    przed="$(rozmiar)"
    # Znacznik z czasem modyfikacji 2001-01-01: wpis odczytany albo zapisany w tym buildzie ma nowszy czas dostępu.
    znacznik="$(mktemp)"
    touch -m -d '2001-01-01 00:00:00 UTC' "$znacznik"
    usuniete="$(find "$katalog" -type f ! -anewer "$znacznik" -print -delete | wc -l)"
    rm -f "$znacznik"
    echo "Cache budowania: usunięto ${usuniete} nieużytych wpisów, ${przed} MB → $(rozmiar) MB." |
      tee -a "${GITHUB_STEP_SUMMARY:-/dev/null}"
    ;;
  *)
    echo "Nieznany tryb ${tryb}; dozwolone: postarz, przytnij." >&2
    exit 2
    ;;
esac
