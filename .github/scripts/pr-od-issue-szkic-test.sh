#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test zamiany PR bez issue w szkic (pr-od-issue-szkic.sh <numer PR>). Zamiast sieci: PR_JSON_FILE i ISSUE_JSON_DIR
# dla kontroli (jak w pr-od-issue-test.sh) oraz własne `gh`, które zapisuje wywołania zmieniające PR do dziennika.
script="$(cd "$(dirname "$0")" && pwd)/pr-od-issue-szkic.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
mkdir "$workdir/bin" "$workdir/issues"
cat >"$workdir/bin/gh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
case "$1 $2" in
  'pr view') printf '%s\n' "$FAKE_DRAFT" ;;
  'pr ready') echo 'szkic' >>"$FAKE_LOG" ;;
  'pr comment') echo 'komentarz' >>"$FAKE_LOG" ;;
  'api -X') echo 'odświeżenie' >>"$FAKE_LOG" ;;
  'api repos/'*) printf '%s\n' "$FAKE_COMMENT_ID" ;;
  *) echo "nieoczekiwane wywołanie gh: $*" >&2; exit 2 ;;
esac
SH
chmod +x "$workdir/bin/gh"
printf '{"state":"OPEN","assignees":[{"login":"ala"}],"url":"https://x/issues/5"}\n' >"$workdir/issues/5.json"

# run_case <nazwa> <tryb wymagany 0/1> <opis PR> <autor> <czy szkic> <id komentarza> <oczekiwany kod> <oczekiwany dziennik> [<tekst w wyjściu>]
run_case() {
  local name="$1" required="$2" body="$3" author="$4" draft="$5" comment="$6" code="$7" expected="$8" text="${9:-}" status=0 actual
  if [[ $body == '@BLAD' ]]; then
    echo 'to nie jest json' >"$workdir/pr.json"
  else
    printf '{"author":{"login":"%s"},"body":"%s","headRefName":"5-opis"}\n' "$author" "$body" >"$workdir/pr.json"
  fi
  : >"$workdir/log.txt"
  PR_JSON_FILE="$workdir/pr.json" ISSUE_JSON_DIR="$workdir/issues" PR_OD_ISSUE_WYMAGANE="$required" \
    GITHUB_REPOSITORY=org/repo FAKE_DRAFT="$draft" FAKE_COMMENT_ID="$comment" FAKE_LOG="$workdir/log.txt" \
    PATH="$workdir/bin:$PATH" bash "$script" 7 >"$workdir/out.txt" 2>&1 || status=$?
  actual="$(cat "$workdir/log.txt")"
  if ((status == code)) && [[ $actual == "$(printf '%b' "$expected")" ]] && { [[ -z $text ]] || grep -qF -- "$text" "$workdir/out.txt"; }; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status} (oczekiwano ${code}), polecenia „${actual//$'\n'/; }”, oczekiwano „${expected}”${text:+ z tekstem „$text”}"
    sed 's/^/    /' "$workdir/out.txt"
    failures=$((failures + 1))
  fi
}

run_case "tryb ostrzegawczy: brak Closes tylko loguje" 0 'Opis.' ala false '' 0 '' '::warning title=PR od issue::brak Closes'
run_case "tryb wymagany: brak Closes → szkic i komentarz" 1 'Opis.' ala false '' 1 'szkic\nkomentarz' '::error title=PR od issue::brak Closes'
run_case "tryb wymagany: PR już szkic, bez ponownej zamiany" 1 'Opis.' ala true '' 1 'komentarz'
run_case "tryb wymagany: komentarz odświeżany, bez duplikatu" 1 'Opis.' ala false 123 1 'szkic\nodświeżenie'
run_case "tryb wymagany: issue przypisane komuś innemu" 1 'Closes #5' ola false '' 1 'szkic\nkomentarz' 'nieprzypisane autorowi'
run_case "tryb wymagany: poprawny PR bez zmian" 1 'Closes #5' ala false '' 0 ''
run_case "tryb wymagany: błąd odczytu danych PR to błąd narzędzia, bez zmiany PR" 1 '@BLAD' ala false '' 2 '' 'Błąd narzędzia kontroli'
run_case "tryb ostrzegawczy: błąd odczytu danych PR też jest błędem narzędzia" 0 '@BLAD' ala false '' 2 '' 'Błąd narzędzia kontroli'
run_case "tryb wymagany: Dependabot pominięty" 1 'Opis.' 'dependabot[bot]' false '' 0 '' 'Dependabota'

exit "$((failures > 0 ? 1 : 0))"
