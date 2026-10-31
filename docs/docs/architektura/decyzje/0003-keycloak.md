# ADR-0003: Usługa logowania w Keycloak

Status: przyjęta. Data: 2026-10-27. Źródło: decyzja projektu nr 7.

## Kontekst i problem

Odwiedzający rejestruje się sam, a Koordynator zaprasza osoby z rolą (US12). Aplikacja nie ma pola hasła (NFR-31), konto blokuje się po nieudanych próbach (NFR-32). Serwer nie przechowuje haseł ani adresów e-mail (NFR-28).

## Rozważone opcje

- Keycloak: gotowy serwer OAuth 2.0 i OpenID Connect z rolami i rejestracją [1].
- Własne konta i tokeny JWT w serwerze aplikacji.
- Firebase Authentication.

## Decyzja

- Keycloak pełni rolę Usługi logowania: przechowuje konta, hasła i role RO-ODW, RO-ZBI, RO-WER i RO-KOO.
- Aplikacja loguje się biblioteką AppAuth [2]: kod autoryzacji z PKCE w przeglądarce systemowej [3].
- Serwer jest serwerem zasobów OAuth 2.0 [4]: sprawdza podpis tokenu i czyta role. Własnego punktu logowania nie ma.
- Konto z zaproszenia serwer zakłada przez Admin REST API Keycloak, kontem technicznym z prawem `manage-users`.
- Role, klienci i konta deweloperskie leżą w `infra/keycloak/realm.json` i wczytują się przy `docker compose up`.
- Strona logowania dostaje własny motyw z dużą czcionką i małą liczbą pól (NFR-15).

## Konsekwencje

- Dobrze: nie piszemy przechowywania haseł ani wydawania tokenów; rejestracja, blokada konta i e-mail z ustawieniem hasła są gotowe.
- Dobrze: aplikacja jest klientem publicznym bez sekretu (NFR-35).
- Gorzej: dodatkowy kontener; według naszego szacunku potrzebuje 0,5–1 GB pamięci.
- Gorzej: konfigurację realmu i motyw strony logowania trzeba utrzymywać i sprawdzać według WCAG.

## Źródła

1. [Keycloak: dokumentacja](https://www.keycloak.org/documentation)
2. [AppAuth for Android](https://github.com/openid/AppAuth-Android)
3. [RFC 8252 — OAuth 2.0 dla aplikacji natywnych](https://www.rfc-editor.org/rfc/rfc8252)
4. [Spring Security: OAuth 2.0 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
