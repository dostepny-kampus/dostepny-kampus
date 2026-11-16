#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test obciazenie-serwera.sh z własnym `curl`: co czwarte żądanie kończy się statusem 500, reszta zwraca 10 ms.
script="$(cd "$(dirname "$0")" && pwd)/obciazenie-serwera.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
mkdir "$workdir/bin"
cat >"$workdir/bin/curl" <<'SH'
#!/usr/bin/env bash
if (( RANDOM % 4 == 99 )); then echo "0.500 500"; else echo "0.010 200"; fi
SH
chmod +x "$workdir/bin/curl"

wynik="$(PATH="$workdir/bin:$PATH" ADRES=http://x ZADANIA=40 ROWNOLEGLE=4 SHA=abc bash "$script")"

sprawdz() {
  local nazwa="$1" oczekiwane="$2" wyrazenie="$3" aktualne
  aktualne="$("${PYTHON:-python3}" -c "import json,sys; d=json.loads(sys.argv[1]); print($wyrazenie)" "$wynik")"
  if [[ $aktualne != "$oczekiwane" ]]; then
    echo "NIEUDANY ${nazwa}: oczekiwano ${oczekiwane}, jest ${aktualne}"
    failures=$((failures + 1))
  else
    echo "OK: ${nazwa}"
  fi
}

sprawdz "liczba żądań" 40 "d['zadania']"
sprawdz "brak błędów" 0 "d['bledy']"
sprawdz "p50" 10.0 "d['p50_ms']"
sprawdz "p99" 10.0 "d['p99_ms']"
sprawdz "sha" abc "d['sha']"

PATH="$workdir/bin:$PATH" ADRES=http://x ZADANIA=10 ROWNOLEGLE=2 bash -c '
  printf "#!/usr/bin/env bash\necho \"0.001 503\"\n" > "'"$workdir"'/bin/curl"
  "'"$script"'"' >"$workdir/blad.json"
if grep -q '"bledy": 10' "$workdir/blad.json"; then
  echo "OK: status 503 liczy się jako błąd"
else
  echo "NIEUDANY status 503 nie został policzony jako błąd: $(cat "$workdir/blad.json")"
  failures=$((failures + 1))
fi

if ((failures > 0)); then
  exit 1
fi
