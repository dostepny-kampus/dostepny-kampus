#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test kontrola-sprintu-wynik.sh. Własne `gh` zwraca historię statusów, otwarte issue i nieudane zadania ze zmiennych,
# a zapis statusu, komentarz, zamknięcie i założenie issue dopisuje do dziennika. Repozytorium ma trzy commity
# z numerami PR w temacie; OD to pierwszy, SHA ostatni.
script="$(cd "$(dirname "$0")" && pwd)/kontrola-sprintu-wynik.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
mkdir "$workdir/bin"
cat >"$workdir/bin/gh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
case "$1 $2" in
  'api -X') for a in "$@"; do [[ $a == state=* ]] && echo "status ${a#state=}" >>"$FAKE_LOG"; done ;;
  'api repos/'*'/statuses') printf '%b' "$FAKE_HISTORIA" ;;
  'api repos/'*'/jobs'*) printf '%b' "$FAKE_NIEUDANE" ;;
  'issue list') printf '%s' "$FAKE_OTWARTE" ;;
  'issue comment' | 'issue close') echo "$2 $3" >>"$FAKE_LOG" ;;
  'issue create')
    while (($#)); do
      [[ $1 == --milestone ]] && echo "create $2" >>"$FAKE_LOG"
      [[ $1 == --body-file ]] && cp "$2" "$FAKE_TRESC"
      shift
    done ;;
  *) echo "nieoczekiwane wywołanie gh: $*" >&2; exit 2 ;;
esac
SH
chmod +x "$workdir/bin/gh"

repo="$workdir/repo"
git init -q "$repo"
for temat in "feat: a (#11)" "fix: b (#12)" "docs: c (#13)"; do
  git -C "$repo" -c user.name=t -c user.email=t@t commit -q --allow-empty -m "$temat"
done
od="$(git -C "$repo" rev-parse HEAD~2)"
sha="$(git -C "$repo" rev-parse HEAD)"

# przypadek <nazwa> <WYNIK> <historia stanów, \n> <otwarte issue> <oczekiwany dziennik, \n> [tekst w treści issue]...
przypadek() {
  local name="$1" status=0 out ok=1
  : >"$workdir/log"
  : >"$workdir/tresc"
  out="$(cd "$repo" && GITHUB_REPOSITORY=org/repo GALAZ=sprint/4 SHA="$sha" OD="$od" WYNIK="$2" RUN_ID=5 ADRES=adres \
    FAKE_HISTORIA="$3" FAKE_OTWARTE="$4" FAKE_NIEUDANE='- `android / emulator`\n' FAKE_LOG="$workdir/log" \
    FAKE_TRESC="$workdir/tresc" PATH="$workdir/bin:$PATH" bash "$script" 2>&1)" || status=$?
  [[ $status == 0 && $(cat "$workdir/log") == "$(printf '%b' "$5")" ]] || ok=0
  for tekst in "${@:6}"; do
    if [[ $tekst == '!'* ]]; then
      ! grep -qF -- "${tekst#!}" "$workdir/tresc" || ok=0
    else
      grep -qF -- "$tekst" "$workdir/tresc" || ok=0
    fi
  done
  if ((ok)); then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status}, dziennik „$(cat "$workdir/log")”, oczekiwano „$5”"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

przypadek "zielona bez otwartego issue: tylko status" success "" "" "status success"
przypadek "zielona z otwartym issue: zamknięcie" success "pending\nfailure\n" 7 "status success\nclose 7"
przypadek "pierwsza porażka: error bez issue" failure "pending\n" "" "status error"
przypadek "anulowanie liczy się jak porażka" cancelled "pending\n" "" "status error"
przypadek "druga porażka bez issue: failure i nowe issue z podejrzanymi" failure "pending\nerror\npending\n" "" \
  "status failure\ncreate Sprint 4" "#12" "#13" '!#11' '`android / emulator`' "### Rozwiązanie" "sprint/4"
przypadek "druga porażka przy otwartym issue: komentarz" failure "pending\nerror\npending\n" 7 \
  "status failure\ncomment 7"

exit "$((failures > 0 ? 1 : 0))"
