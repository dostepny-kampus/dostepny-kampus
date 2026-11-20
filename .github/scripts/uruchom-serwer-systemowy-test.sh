#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test uruchom-serwer-systemowy.sh na atrapach docker, java i curl: kolejność startu usług, konto magazynu
# osobno od --wait, serwer z pliku JAR bez -plain, wpis budynku TS-1 w bazie i błąd z dziennikiem, gdy serwer
# nie odpowiada.
script="$(cd "$(dirname "$0")" && pwd)/uruchom-serwer-systemowy.sh"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
mkdir -p "$test_dir/bin" "$test_dir/repo/infra" "$test_dir/repo/backend/target" "$test_dir/tmp"
printf 'DK_TEST=1\nDK_DB_USER=u\nDK_DB_NAME=db\n' > "$test_dir/repo/infra/.env.example"
touch "$test_dir/repo/backend/target/serwer.jar" "$test_dir/repo/backend/target/serwer-plain.jar"
printf '#!/usr/bin/env bash\nprintf "docker %%s\n" "$*" >> "%s/log"\n' "$test_dir" > "$test_dir/bin/docker"
printf '#!/usr/bin/env bash\nprintf "java %%s DK_TEST=%%s\n" "$*" "$DK_TEST" >> "%s/log"\necho dziennik-serwera\n' \
  "$test_dir" > "$test_dir/bin/java"
printf '#!/usr/bin/env bash\nexit "${CURL_KOD:-0}"\n' > "$test_dir/bin/curl"
chmod +x "$test_dir/bin/"*
export PATH="$test_dir/bin:$PATH" RUNNER_TEMP="$test_dir/tmp"
cd "$test_dir/repo"

bash "$script"
sleep 1
start_lines="docker compose -f infra/docker-compose.yml up -d --wait postgis zdjecia logowanie
docker compose -f infra/docker-compose.yml run --rm zdjecia-konto"
if [[ $(head -n 2 "$test_dir/log") != "$start_lines" ]]; then
  echo "BŁĄD: kolejność startu „$(cat "$test_dir/log")”, oczekiwano „${start_lines}”"
  exit 1
fi
if ! grep -Fxq 'java -jar backend/target/serwer.jar DK_TEST=1' "$test_dir/log"; then
  echo "BŁĄD: brak wywołania java z DK_TEST=1 w „$(cat "$test_dir/log")”"
  exit 1
fi
if ! grep -q "docker compose -f infra/docker-compose.yml exec -T postgis psql -v ON_ERROR_STOP=1 -U u -d db -c INSERT INTO buildings .*'TS-1'" "$test_dir/log"; then
  echo "BŁĄD: brak wpisu budynku TS-1 przez psql w „$(cat "$test_dir/log")”"
  exit 1
fi
echo "OK: usługi z --wait, konto magazynu osobno, serwer z JAR bez -plain i wpis budynku TS-1 w bazie"

: > "$test_dir/log"
if out="$(CURL_KOD=7 CZAS_STARTU=1 bash "$script")"; then
  echo "BŁĄD: skrypt przeszedł, choć serwer nie odpowiadał"
  exit 1
fi
if [[ $out != *"::error title=Test systemowy::"* || $out != *dziennik-serwera* ]]; then
  echo "BŁĄD: brak błędu albo dziennika serwera w „${out}”"
  exit 1
fi
if grep -q 'psql' "$test_dir/log"; then
  echo "BŁĄD: skrypt wpisał budynek TS-1 mimo braku odpowiedzi serwera"
  exit 1
fi
echo "OK: brak odpowiedzi serwera kończy się błędem z dziennikiem bez wpisu w bazie"
