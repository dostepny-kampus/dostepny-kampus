# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Dostępny Kampus — kontrola tokenów wizualnych (kontrast WCAG 2.1, daltonizm, typografia, wymiary) i tabele strony tokenów. Author: Vladyslav Yevkov. Version: 0.1.0. Copyright (c) 2026 Vladyslav Yevkov."""

from __future__ import annotations

import argparse
import math
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import yaml

from check_ids import aligned_table, updated_content, wrap_block

REPO = Path(__file__).resolve().parent.parent
DEFAULT_TOKENS = REPO / "docs" / "ux" / "tokeny.yml"
THEMES = (("jasny", "Jasny"), ("ciemny", "Ciemny"))
HEX = re.compile(r"^#[0-9A-Fa-f]{6}$")
# Najniższe progi z NFR-16 (WCAG 1.4.3 i 1.4.11): progu w tokeny.yml nie wolno ustawić niżej.
NFR_MINIMUM = {"tekst": 4.5, "komponent": 3.0}
MIN_BODY_SP = 18
# Próg zespołu dla najmniejszego tekstu: minimum_sp w tokeny.yml nie może zejść niżej.
MIN_TEXT_SP = 16
MIN_TARGET_DP = 48
BODY_STYLE = "bodyLarge"
DIMENSION_GROUPS = (
    ("odstepy", "Odstępy"),
    ("cele_dotyku", "Cele dotyku"),
    ("obramowania", "Obramowania"),
    ("zaokraglenia", "Zaokrąglenia"),
    ("ikony", "Ikony"),
)
# Macierze symulacji daltonizmu (Machado, Oliveira, Fernandes 2009, nasilenie 1,0), stosowane do RGB liniowego.
SIMULATIONS = {
    "protanopia": ((0.152286, 1.052583, -0.204868), (0.114503, 0.786281, 0.099216), (-0.003882, -0.048116, 1.051998)),
    "deuteranopia": ((0.367322, 0.860646, -0.227968), (0.280085, 0.672501, 0.047413), (-0.011820, 0.042940, 0.968881)),
}
SWATCH = (
    '<span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;'
    'border:1px solid #808080;border-radius:4px;background:{color}"></span> `{color}`'
)


class TokenError(Exception):
    """Błąd struktury pliku tokenów (brak klucza, zły kolor, nieznana rola)."""


@dataclass(frozen=True)
class PairResult:
    """Wynik jednej pary w jednym motywie: kontrast zwykły i najniższy po symulacji daltonizmu."""

    theme: str
    color: str
    background: str
    kind: str
    threshold: float
    normal: float
    simulated: float

    @property
    def worst(self) -> float:
        """Najniższy kontrast pary: zwykły albo po symulacji."""
        return min(self.normal, self.simulated)

    @property
    def ok(self) -> bool:
        """True, gdy para spełnia próg także po symulacji daltonizmu."""
        return self.worst >= self.threshold


def channel_to_linear(value: float) -> float:
    """Zwraca składową RGB liniową dla składowej sRGB z przedziału 0–1 (definicja luminancji względnej WCAG)."""
    return value / 12.92 if value <= 0.04045 else ((value + 0.055) / 1.055) ** 2.4


def linear_rgb(color: str) -> tuple[float, float, float]:
    """Zwraca liniowe RGB koloru #RRGGBB."""
    r, g, b = (channel_to_linear(int(color[i : i + 2], 16) / 255) for i in (1, 3, 5))
    return r, g, b


def luminance(rgb: tuple[float, float, float]) -> float:
    """Zwraca luminancję względną WCAG dla liniowego RGB, obciętego do przedziału 0–1."""
    r, g, b = (min(1.0, max(0.0, c)) for c in rgb)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def simulate(rgb: tuple[float, float, float], matrix: tuple[tuple[float, ...], ...]) -> tuple[float, float, float]:
    """Zwraca liniowe RGB po symulacji daltonizmu macierzą `matrix`."""
    r, g, b = (sum(m * c for m, c in zip(row, rgb)) for row in matrix)
    return r, g, b


def ratio(l1: float, l2: float) -> float:
    """Zwraca współczynnik kontrastu WCAG (L1 + 0,05) / (L2 + 0,05), gdzie L1 to jaśniejsza luminancja."""
    high, low = max(l1, l2), min(l1, l2)
    return (high + 0.05) / (low + 0.05)


def contrast(color: str, background: str) -> float:
    """Zwraca współczynnik kontrastu dwóch kolorów #RRGGBB."""
    return ratio(luminance(linear_rgb(color)), luminance(linear_rgb(background)))


def simulated_contrast(color: str, background: str) -> float:
    """Zwraca najniższy kontrast pary po symulacji każdego typu daltonizmu z SIMULATIONS."""
    fg, bg = linear_rgb(color), linear_rgb(background)
    return min(ratio(luminance(simulate(fg, m)), luminance(simulate(bg, m))) for m in SIMULATIONS.values())


def floor2(value: float) -> float:
    """Zwraca wartość obciętą w dół do 2 miejsc po przecinku, by tabela nigdy nie zawyżała wyniku."""
    return math.floor(value * 100) / 100


def fmt_ratio(value: float) -> str:
    """Zwraca kontrast w zapisie polskim, np. `8,40:1`."""
    return f"{floor2(value):.2f}".replace(".", ",") + ":1"


def fmt_number(value: float) -> str:
    """Zwraca liczbę w zapisie polskim bez zbędnych zer, np. `7` albo `4,5`."""
    return f"{value:g}".replace(".", ",")


def load_tokens(path: Path) -> dict[str, Any]:
    """Wczytuje plik tokenów. Wyjątki: OSError, yaml.YAMLError, TokenError przy pliku, który nie jest mapą."""
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise TokenError(f"{path}: plik nie zawiera mapy tokenów")
    return data


def require(data: dict[str, Any], key: str, where: str) -> Any:
    """Zwraca `data[key]`. Wyjątki: TokenError, gdy klucza brak."""
    if not isinstance(data, dict) or key not in data:
        raise TokenError(f"brak klucza `{key}` w {where}")
    return data[key]


def check_colors(colors: dict[str, Any]) -> list[str]:
    """Zwraca błędy kolorów: każda rola ma poprawny kolor #RRGGBB w obu motywach."""
    errors = []
    for role, entry in colors.items():
        for theme, _ in THEMES:
            value = entry.get(theme) if isinstance(entry, dict) else None
            if not (isinstance(value, str) and HEX.match(value)):
                errors.append(f"kolor {role}.{theme}: oczekiwano #RRGGBB, jest {value!r}")
    return errors


def check_thresholds(thresholds: dict[str, Any]) -> list[str]:
    """Zwraca błędy progów: każdy rodzaj z NFR_MINIMUM ma próg nie niższy niż w NFR-16."""
    errors = []
    for kind, minimum in NFR_MINIMUM.items():
        value = thresholds.get(kind)
        if not isinstance(value, (int, float)):
            errors.append(f"próg `{kind}`: brak wartości liczbowej")
        elif value < minimum:
            errors.append(f"próg `{kind}` = {fmt_number(value)} jest niższy niż {fmt_number(minimum)} z NFR-16")
    return errors


def evaluate_pairs(tokens: dict[str, Any]) -> tuple[list[PairResult], list[str]]:
    """Zwraca wyniki wszystkich par w obu motywach oraz błędy definicji par (nieznana rola albo rodzaj)."""
    colors = require(tokens, "kolory", "pliku")
    section = require(tokens, "kontrast", "pliku")
    thresholds = require(section, "progi", "kontrast")
    results, errors = [], []
    for index, pair in enumerate(require(section, "pary", "kontrast"), start=1):
        fg, bg, kind = pair.get("kolor"), pair.get("tlo"), pair.get("rodzaj")
        unknown = [name for name in (fg, bg) if name not in colors]
        if unknown or kind not in thresholds:
            errors.append(f"para {index}: nieznana rola {unknown} albo rodzaj {kind!r}")
            continue
        for theme, _ in THEMES:
            c, b = colors[fg][theme], colors[bg][theme]
            results.append(PairResult(theme, fg, bg, kind, float(thresholds[kind]), contrast(c, b), simulated_contrast(c, b)))
    return results, errors


def check_typography(typography: dict[str, Any]) -> list[str]:
    """Zwraca błędy typografii: minimum_sp ≥ 16, każdy styl ≥ minimum_sp, tekst podstawowy ≥ 18 sp, interlinia ≥ rozmiar."""
    errors = []
    minimum = require(typography, "minimum_sp", "typografia")
    styles = require(typography, "style", "typografia")
    if minimum < MIN_TEXT_SP:
        errors.append(f"minimum_sp = {minimum} sp jest niższe niż próg zespołu {MIN_TEXT_SP} sp")
    for name, style in styles.items():
        if style["sp"] < minimum:
            errors.append(f"styl {name}: {style['sp']} sp < minimum {minimum} sp")
        if style["interlinia_sp"] < style["sp"]:
            errors.append(f"styl {name}: interlinia {style['interlinia_sp']} sp mniejsza niż rozmiar")
    body = require(styles, BODY_STYLE, "typografia.style")
    if body["sp"] < MIN_BODY_SP:
        errors.append(f"tekst podstawowy ({BODY_STYLE}) {body['sp']} sp < {MIN_BODY_SP} sp (konwencje makiet)")
    return errors


def check_dimensions(tokens: dict[str, Any]) -> list[str]:
    """Zwraca błędy wymiarów: odstępy na siatce, cele dotyku ≥ 48 dp (NFR-17)."""
    errors = []
    grid = require(tokens, "siatka_dp", "pliku")
    for name, entry in require(tokens, "odstepy", "pliku").items():
        if entry["dp"] % grid:
            errors.append(f"odstęp {name}: {entry['dp']} dp nie jest wielokrotnością siatki {grid} dp")
    targets = require(tokens, "cele_dotyku", "pliku")
    for name in ("minimum", "przycisk"):
        value = require(targets, name, "cele_dotyku")["dp"]
        if value < MIN_TARGET_DP:
            errors.append(f"cel dotyku {name}: {value} dp < {MIN_TARGET_DP} dp (NFR-17)")
    return errors


def color_rows(tokens: dict[str, Any]) -> list[list[str]]:
    """Zwraca wiersze tabeli kolorów: rola, próbka jasna, próbka ciemna, zastosowanie, źródło."""
    rows = []
    for role, entry in tokens["kolory"].items():
        rows.append([f"`{role}`", *(SWATCH.format(color=entry[t]) for t, _ in THEMES), entry["uzycie"], entry["zrodlo"]])
    return rows


def contrast_rows(tokens: dict[str, Any], results: list[PairResult]) -> list[list[str]]:
    """Zwraca wiersze tabeli kontrastu: para, zastosowanie, próg, wyniki w obu motywach (zwykły i po symulacji)."""
    by_key = {(r.theme, r.color, r.background, r.kind): r for r in results}
    rows = []
    for pair in tokens["kontrast"]["pary"]:
        cells = [f"`{pair['kolor']}` na `{pair['tlo']}`", pair["gdzie"], "≥ " + fmt_number(tokens["kontrast"]["progi"][pair["rodzaj"]]) + ":1"]
        for theme, _ in THEMES:
            r = by_key[(theme, pair["kolor"], pair["tlo"], pair["rodzaj"])]
            cells += [fmt_ratio(r.normal), fmt_ratio(r.simulated)]
        rows.append(cells)
    return rows


def typography_rows(tokens: dict[str, Any]) -> list[list[str]]:
    """Zwraca wiersze tabeli typografii: styl, rozmiar, waga, interlinia, zastosowanie, źródło."""
    return [
        [f"`{name}`", f"{s['sp']} sp", str(s["waga"]), f"{s['interlinia_sp']} sp", s["uzycie"], s["zrodlo"]]
        for name, s in tokens["typografia"]["style"].items()
    ]


def dimension_rows(tokens: dict[str, Any]) -> list[list[str]]:
    """Zwraca wiersze tabeli wymiarów: grupa, token, wartość, zastosowanie, źródło."""
    rows = []
    for key, title in DIMENSION_GROUPS:
        for name, entry in tokens[key].items():
            rows.append([title, f"`{key}.{name}`", f"{entry['dp']} dp", entry["uzycie"], entry["zrodlo"]])
    return rows


def preview(tokens: dict[str, Any], theme: str, title: str) -> str:
    """Zwraca podgląd motywu w HTML: tytuł, wejścia w ramce, czynność główna, przycisk drugorzędny, błąd i podpis.

    Gwarantuje: jeden wiersz HTML bez pustych linii (blok surowego HTML w Markdown), wyłącznie znaczniki div i span,
    rozmiary w px równe rozmiarom w sp i dp z tokenów.
    """
    c = {role: entry[theme] for role, entry in tokens["kolory"].items()}
    st = tokens["typografia"]["style"]
    bold = tokens["typografia"]["wyroznienie_waga"]
    gap, side = tokens["odstepy"]["s"]["dp"], tokens["odstepy"]["m"]["dp"]
    border, radius = tokens["obramowania"]["ramka"]["dp"], tokens["zaokraglenia"]["standard"]["dp"]
    button = tokens["cele_dotyku"]["przycisk"]["dp"]

    def text(style: str, color: str, content: str, weight: int | None = None) -> str:
        s = st[style]
        return (
            f'<div style="font-size:{s["sp"]}px;line-height:{s["interlinia_sp"]}px;'
            f'font-weight:{weight or s["waga"]};color:{c[color]}">{content}</div>'
        )

    def entrance(icon: str, label: str, name: str, source: str) -> str:
        return text("bodyLarge", "onSurface", f"{icon} {label}: {name}") + text("bodyLarge", "onSurfaceVariant", source)

    frame = (
        f'<div style="background:{c["surfaceVariant"]};border:{border}px solid {c["outline"]};'
        f'border-radius:{radius}px;padding:{gap}px;margin:{gap}px 0">'
        + entrance("✗", "Niedostępne", "Wejście główne", "Sprawdzone na miejscu [data]")
        + entrance("✓", "Dostępne", "Wejście bramą od ul. Norwida", "Sprawdzone na miejscu [data]")
        + entrance("–", "Brak danych", "[nazwa wejścia]", "Z map i opisów, stan z [data]")
        + "</div>"
    )
    label = st["labelLarge"]
    btn = (
        f"display:block;min-height:{button}px;line-height:{button}px;text-align:center;"
        f'border-radius:{radius}px;margin:{side}px 0 0 0;font-size:{label["sp"]}px'
    )
    filled_border = tokens["obramowania"]["wypelniony"]["dp"]
    filled = (
        f'background:{c["primaryContainer"]};color:{c["onPrimaryContainer"]};'
        f'border:{filled_border}px solid {c["primary"]}'
        if theme == "ciemny"
        else f'background:{c["primary"]};color:{c["onPrimary"]}'
    )
    primary = f'<div style="{btn};{filled};font-weight:{bold}">Sprawdź, którędy wejść</div>'
    secondary = (
        f'<div style="{btn};border:{border}px solid {c["outline"]};color:{c["primary"]};'
        f'font-weight:{label["waga"]}">Zmień oznaczenie</div>'
    )
    return (
        f'<div style="display:inline-block;vertical-align:top;box-sizing:border-box;width:100%;max-width:360px;'
        f'margin:0 {side}px {side}px 0;padding:{side}px;background:{c["surface"]};'
        f'border:1px solid #808080;border-radius:{radius}px">'
        + text("bodyMedium", "onSurfaceVariant", f"Motyw {title.lower()}")
        + text("titleLarge", "onSurface", "Budynek A-1")
        + frame
        + primary
        + secondary
        + f'<div style="margin-top:{side}px"></div>'
        + text("bodyLarge", "error", "Dodaj zdjęcie wejścia, aby wysłać zmianę.", bold)
        + f'<div style="margin-top:{gap}px"></div>'
        + text("bodyMedium", "onSurfaceVariant", "Źródło: Politechnika Wrocławska")
        + "</div>"
    )


def render_blocks(tokens: dict[str, Any], results: list[PairResult]) -> dict[str, str]:
    """Zwraca wygenerowane bloki strony tokenów (nazwa → blok ze znacznikami i wyłączeniem Vale)."""
    contrast_header = ["Para", "Zastosowanie", "Próg"]
    for _, title in THEMES:
        contrast_header += [title, f"{title}, daltonizm"]
    bodies = {
        "kolory": aligned_table(["Rola", "Jasny", "Ciemny", "Zastosowanie", "Źródło"], color_rows(tokens)),
        "podglad": ["".join(preview(tokens, theme, title) for theme, title in THEMES)],
        "kontrast": aligned_table(contrast_header, contrast_rows(tokens, results)),
        "typografia": aligned_table(["Styl", "Rozmiar", "Waga", "Interlinia", "Zastosowanie", "Źródło"], typography_rows(tokens)),
        "wymiary": aligned_table(["Grupa", "Token", "Wartość", "Zastosowanie", "Źródło"], dimension_rows(tokens)),
    }
    return {name: wrap_block(name, body) for name, body in bodies.items()}


def print_results(results: list[PairResult]) -> None:
    """Wypisuje tabelę wyników par: motyw, para, rodzaj, próg, kontrast zwykły, po symulacji, wynik."""
    print(f"{'Motyw':7} {'Para':36} {'Rodzaj':9} {'Próg':>6} {'Zwykły':>8} {'P/D':>8}  Wynik")
    for r in results:
        pair = f"{r.color} na {r.background}"
        status = "PASS" if r.ok else "FAIL"
        print(f"{r.theme:7} {pair:36} {r.kind:9} {r.threshold:>6.1f} {floor2(r.normal):>8.2f} {floor2(r.simulated):>8.2f}  {status}")


def check_all(tokens: dict[str, Any]) -> tuple[list[PairResult], list[str]]:
    """Wykonuje wszystkie kontrole tokenów i zwraca wyniki par oraz listę błędów (pusta, gdy wszystko spełnia progi)."""
    errors = check_colors(require(tokens, "kolory", "pliku"))
    errors += check_thresholds(require(require(tokens, "kontrast", "pliku"), "progi", "kontrast"))
    if errors:
        return [], errors
    results, pair_errors = evaluate_pairs(tokens)
    errors += pair_errors
    used = {name for pair in tokens["kontrast"]["pary"] for name in (pair.get("kolor"), pair.get("tlo"))}
    errors += [f"rola {role} nie występuje w żadnej parze kontrastu" for role in tokens["kolory"] if role not in used]
    errors += [
        f"kontrast {r.theme}: {r.color} na {r.background} = {fmt_ratio(r.worst)} < {fmt_number(r.threshold)}:1 ({r.kind})"
        for r in results
        if not r.ok
    ]
    errors += check_typography(require(tokens, "typografia", "pliku"))
    errors += check_dimensions(tokens)
    return results, errors


def page_is_current(page: Path, blocks: dict[str, str]) -> bool:
    """Zwraca True, gdy strona istnieje i jej bloki są takie jak `blocks`. Wyjątki: OSError przy błędzie odczytu."""
    if not page.is_file():
        return False
    current = page.read_text(encoding="utf-8")
    return current == updated_content(current, blocks)


def write_page(page: Path, blocks: dict[str, str]) -> None:
    """Wstawia bloki do strony (zastępuje istniejące, brakujące dopisuje na końcu). Wyjątki: OSError."""
    current = page.read_text(encoding="utf-8") if page.is_file() else ""
    page.write_text(updated_content(current, blocks), encoding="utf-8", newline="\n")


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    """Parsuje argumenty wiersza poleceń."""
    parser = argparse.ArgumentParser(description="Kontrola tokenów wizualnych: kontrast WCAG 2.1, daltonizm, typografia, wymiary.")
    parser.add_argument("--tokens", type=Path, default=DEFAULT_TOKENS, help="plik tokenów YAML")
    target = parser.add_mutually_exclusive_group()
    target.add_argument("--page", type=Path, help="po udanej kontroli zapisz tabele do strony Markdown")
    target.add_argument("--check-page", type=Path, help="sprawdź także, czy tabele na stronie są aktualne")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    """Punkt wejścia. Zwraca 0, gdy tokeny spełniają wszystkie progi (i strona jest aktualna z --check-page),
    1 przy niespełnionym progu albo nieaktualnej stronie, 2 przy błędzie odczytu lub struktury pliku."""
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")
    args = parse_args(argv)
    try:
        tokens = load_tokens(args.tokens)
        results, errors = check_all(tokens)
    except (OSError, yaml.YAMLError, TokenError, KeyError, TypeError) as exc:
        print(f"kontrast: BŁĄD: {type(exc).__name__}: {exc}", file=sys.stderr)
        return 2
    if results:
        print_results(results)
    for error in errors:
        print(f"kontrast: BŁĄD: {error}")
    print(f"kontrast: par {len(results) // len(THEMES)} × {len(THEMES)} motywy, błędów {len(errors)}")
    if errors:
        return 1
    blocks = render_blocks(tokens, results)
    try:
        if args.page:
            write_page(args.page, blocks)
            print(f"kontrast: tabele zapisane do {args.page}")
        elif args.check_page and not page_is_current(args.check_page, blocks):
            print(f"kontrast: BŁĄD: tabele w {args.check_page} są nieaktualne; odśwież: python scripts/kontrast.py --page {args.check_page}")
            return 1
        elif args.check_page:
            print(f"kontrast: tabele w {args.check_page} są aktualne")
    except OSError as exc:
        print(f"kontrast: BŁĄD: {exc}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
