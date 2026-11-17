#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
# Test sprawdzenia podpisu apk-debug (podpis-apk.sh) na udawanym apksigner: plik „apk” zawiera wynik
# `verify --print-certs`, a plik z końcówką .zly kończy weryfikację błędem.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
script="$here/podpis-apk.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
klucz="2d15685ce588a93efed7a925933bf41bdb18d07d038061916e6bb99a604bdcec"
inny="df2545db4bbe7f548615363bca376d3433d2dbb45567bd38351c4927b54b0438"

cat >"$workdir/apksigner" <<'SKRYPT'
#!/usr/bin/env bash
apk="${!#}"
if [[ $apk == *.zly ]]; then
  echo "DOES NOT VERIFY" >&2
  echo "ERROR: Missing META-INF/MANIFEST.MF" >&2
  exit 1
fi
cat "$apk"
SKRYPT
chmod +x "$workdir/apksigner"

# run_case <nazwa> <oczekiwany kod> <klucz> <apk…>
run_case() {
  local name="$1" expected="$2" key="$3" status=0 out
  shift 3
  out="$(APKSIGNER="$workdir/apksigner" bash "$script" "$key" "$@" 2>&1)" || status=$?
  if ((status == expected)); then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

printf 'Signer #1 certificate DN: CN=Android Debug\nSigner #1 certificate SHA-256 digest: %s\n' "$klucz" >"$workdir/ten.apk"
printf 'WARNING: native access\nSigner #1 certificate SHA-256 digest: %s\r\n' "$klucz" >"$workdir/crlf.apk"
printf 'Signer (minSdkVersion=26, maxSdkVersion=2147483647) certificate SHA-256 digest: %s\n' "$klucz" \
  >"$workdir/przedrostek.apk"
printf 'V2 Signer: certificate DN: CN=Android Debug\nV2 Signer: certificate SHA-256 digest: %s\n' "$klucz" >"$workdir/v2.apk"
wielkie="$(printf '%s' "$klucz" | tr 'a-f' 'A-F' | sed 's/../&:/g; s/:$//')"
printf 'Signer #1 certificate SHA-256 digest: %s\n' "$wielkie" >"$workdir/wielkie.apk"
printf 'Signer #1 certificate SHA-256 digest: %s\n' "$inny" >"$workdir/inny.apk"
printf 'Verifies\n' >"$workdir/bez-certyfikatu.apk"
: >"$workdir/apk.zly"

run_case "certyfikat klucza z repozytorium" 0 "$klucz" "$workdir/ten.apk"
run_case "koniec wiersza CRLF i ostrzeżenia Javy" 0 "$klucz" "$workdir/crlf.apk"
run_case "inny przedrostek wiersza podpisującego" 0 "$klucz" "$workdir/przedrostek.apk"
run_case "wiersz V2 Signer z build-tools 37" 0 "$klucz" "$workdir/v2.apk"
run_case "wielkie litery i dwukropki w skrócie" 0 "$klucz" "$workdir/wielkie.apk"
run_case "trzy apk z tym samym kluczem" 0 "$klucz" "$workdir/ten.apk" "$workdir/crlf.apk" "$workdir/wielkie.apk"
run_case "inny certyfikat" 1 "$klucz" "$workdir/inny.apk"
run_case "drugi z dwóch apk z innym certyfikatem" 1 "$klucz" "$workdir/ten.apk" "$workdir/inny.apk"
run_case "wynik bez skrótu certyfikatu" 1 "$klucz" "$workdir/bez-certyfikatu.apk"
run_case "apk nie przechodzi weryfikacji" 1 "$klucz" "$workdir/apk.zly"
run_case "pusty skrót klucza" 1 "" "$workdir/ten.apk"
run_case "skrót klucza z dwukropkami" 1 "$wielkie" "$workdir/ten.apk"
run_case "bez plików apk" 1 "$klucz"

if ((failures > 0)); then
  echo "Nieudanych przypadków: ${failures}"
  exit 1
fi
