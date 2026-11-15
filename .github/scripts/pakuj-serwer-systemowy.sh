#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Uruchamiany z backend/. Pakuje serwer bez testów i qulice; pobranie zależności ponawia mvn-z-ponowieniem.sh.
exec bash "$(dirname "${BASH_SOURCE[0]}")/mvn-z-ponowieniem.sh" "$@" -B -q -DskipTests -Dqulice.skip=true -Djacoco.skip=true package
