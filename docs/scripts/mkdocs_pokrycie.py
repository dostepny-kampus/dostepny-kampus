#!/usr/bin/env python3
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Dostępny Kampus — zaczep MkDocs: strona pokrycia scenariuszy testami powstaje przy każdej budowie dokumentacji."""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import check_ids  # noqa: E402  (ścieżka do skryptu dodana wyżej)


def on_pre_build(config: object) -> None:
    """Zapisuje stronę pokrycia z bieżących scenariuszy i testów, zanim MkDocs zbierze pliki dokumentacji.

    Gwarantuje: strona odpowiada plikom w katalogu roboczym; nie jest przechowywana w repozytorium.
    Wyjątki: OSError/UnicodeDecodeError przy błędzie odczytu albo zapisu zatrzymują budowę.
    """
    docs = Path(config["docs_dir"])
    root = check_ids.ROOT
    check_ids.write_coverage(docs, check_ids.collect(docs, root / "specs", root / "android", root / "backend"))
