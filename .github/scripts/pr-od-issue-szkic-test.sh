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
  'pr ready')
    case "${FAKE_READY_FAIL:-}" in
      '') echo 'szkic' >>"$FAKE_LOG" ;;
      uprawnienia) echo 'GraphQL: Resource not accessible by integration (convertPullRequestToDraft)' >&2; exit 1 ;;
      *) echo 'HTTP 502: Bad Gateway' >&2; exit 1 ;;
    esac ;;
  'pr comment') echo 'komentarz' >>"$FAKE_LOG"; printf '%s' "${*: -1}" >"$FAKE_BODY" ;;
  'api -X') echo 'odświeżenie' >>"$FAKE_LOG"; printf '%s' "${*: -1}" >"$FAKE_BODY" ;;
  'api repos/'*) printf '%s\n' "$FAKE_COMMENT_ID" ;;
  *) echo "nieoczekiwane wywołanie gh: $*" >&2; exit 2 ;;
esac
SH
chmod +x "$workdir/bin/gh"
printf '{"state":"OPEN","assignees":[{"login":"ala"}],"url":"https://x/issues/5"}\n' >"$workdir/issues/5.json"

# run_case <nazwa> <tryb wymagany 0/1> <opis PR> <autor> <czy szkic> <id komentarza> <oczekiwany kod> <oczekiwany dziennik> [<tekst w wyjściu> [<tekst w komentarzu> [<tekst, którego w komentarzu nie ma>]]]
run_case() {
  local name="$1" required="$2" body="$3" author="$4" draft="$5" comment="$6" code="$7" expected="$8" text="${9:-}" inbody="${10:-}" notinbody="${11:-}" status=0 actual
  if [[ $body == '@BLAD' ]]; then
    echo 'to nie jest json' >"$workdir/pr.json"
  else
    printf '{"author":{"login":"%s"},"body":"%s","headRefName":"5-opis","labels":[{"name":"ci"}]}\n' "$author" "$body" >"$workdir/pr.json"
  fi
  : >"$workdir/log.txt"
  : >"$workdir/body.txt"
  PR_JSON_FILE="$workdir/pr.json" ISSUE_JSON_DIR="$workdir/issues" PR_OD_ISSUE_WYMAGANE="$required" \
    GITHUB_REPOSITORY=org/repo FAKE_DRAFT="$draft" FAKE_COMMENT_ID="$comment" FAKE_LOG="$workdir/log.txt" FAKE_BODY="$workdir/body.txt" \
    PATH="$workdir/bin:$PATH" bash "$script" 7 >"$workdir/out.txt" 2>&1 || status=$?
  actual="$(cat "$workdir/log.txt")"
  if ((status == code)) && [[ $actual == "$(printf '%b' "$expected")" ]] && { [[ -z $text ]] || grep -qF -- "$text" "$workdir/out.txt"; } &&
    { [[ -z $inbody ]] || grep -qF -- "$inbody" "$workdir/body.txt"; } && { [[ -z $notinbody ]] || ! grep -qF -- "$notinbody" "$workdir/body.txt"; }; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status} (oczekiwano ${code}), polecenia „${actual//$'\n'/; }”, oczekiwano „${expected}”${text:+ z tekstem „$text”}${inbody:+, w komentarzu „$inbody”}${notinbody:+, bez „$notinbody” w komentarzu}"
    sed 's/^/    /' "$workdir/out.txt"
    failures=$((failures + 1))
  fi
}

run_case "tryb ostrzegawczy: brak Closes tylko loguje" 0 'Opis.' ala false '' 0 '' '::warning title=PR od issue::brak Closes'
run_case "tryb wymagany: brak Closes → szkic i komentarz" 1 'Opis.' ala false '' 1 'szkic\nkomentarz' '::error title=PR od issue::brak Closes' 'i jest szkicem' 'nie udało się zamienić'
run_case "tryb wymagany: PR już szkic, bez ponownej zamiany" 1 'Opis.' ala true '' 1 'komentarz'
run_case "tryb wymagany: komentarz odświeżany, bez duplikatu" 1 'Opis.' ala false 123 1 'szkic\nodświeżenie'
run_case "tryb wymagany: issue przypisane komuś innemu" 1 'Closes #5' ola false '' 1 'szkic\nkomentarz' 'nieprzypisane autorowi'
run_case "tryb wymagany: poprawny PR bez zmian" 1 'Closes #5' ala false '' 0 ''
FAKE_READY_FAIL=uprawnienia run_case "tryb wymagany: token bez prawa do szkicu, komentarz mówi prawdę" 1 'Opis.' ala false '' 1 'komentarz' 'Nie udało się zamienić' 'nie udało się zamienić go w szkic' 'jest szkicem'
FAKE_READY_FAIL=siec run_case "tryb wymagany: inny błąd zamiany w szkic to błąd narzędzia (kod 2), bez komentarza" 1 'Opis.' ala false '' 2 '' 'Błąd narzędzia kontroli'
run_case "tryb wymagany: błąd odczytu danych PR to błąd narzędzia, bez zmiany PR" 1 '@BLAD' ala false '' 2 '' 'Błąd narzędzia kontroli'
run_case "tryb ostrzegawczy: błąd odczytu danych PR też jest błędem narzędzia" 0 '@BLAD' ala false '' 2 '' 'Błąd narzędzia kontroli'
run_case "tryb wymagany: Dependabot pominięty" 1 'Opis.' 'dependabot[bot]' false '' 0 '' 'Dependabota'

exit "$((failures > 0 ? 1 : 0))"
