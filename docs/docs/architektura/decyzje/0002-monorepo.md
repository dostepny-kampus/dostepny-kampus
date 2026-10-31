# ADR-0002: Jedno repozytorium

Status: przyjęta. Data: 2026-10-28. Źródło: decyzje projektu nr 4, 5 i 40.

## Kontekst i problem

Kontrakt API, serwer, aplikacja, dane budynków, scenariusze akceptacyjne i dokumentacja zmieniają się razem. Zespół ma trzy osoby, a PR zadania ma najwyżej 300 zmienionych linii (NFR-46).

## Rozważone opcje

- Jedno repozytorium z katalogiem na każdą część systemu.
- Osobne repozytoria serwera i aplikacji, dokumentacja w trzecim.

## Decyzja

- Jedno publiczne repozytorium z katalogami `api/`, `backend/`, `android/`, `data/`, `specs/`, `infra/` i `docs/`.
- Kontrakt `api/openapi.yaml` powstaje przed kodem (NFR-44). Z niego generujemy interfejsy serwera i klienta.
- Każdy przepływ CI startuje tylko przy zmianach w swoich ścieżkach [2].
- Gałęzie `main` ← `sprint/N` ← `feature/*`, commity według Conventional Commits [1].

## Konsekwencje

- Dobrze: zmiana kontraktu, serwera i aplikacji wchodzi jednym PR, więc te części się nie rozjadą.
- Dobrze: jedna historia i jedno CI; scenariusze z `specs/` leżą obok kodu, który je wykonuje.
- Dobrze: dokumentacja ma historię i kopię razem z kodem.
- Gorzej: dwa systemy budowania w jednym repozytorium.
- Gorzej: przepływ bez filtra ścieżek budowałby wszystko przy każdej zmianie.
- Gorzej: uprawnienia nadaje się do całego repozytorium, nie do katalogu.

## Źródła

1. [Conventional Commits 1.0.0](https://www.conventionalcommits.org/pl/v1.0.0/)
2. [GitHub Actions: składnia przepływów, filtr paths](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)
