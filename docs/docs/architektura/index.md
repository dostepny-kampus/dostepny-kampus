# Architektura

Ta część pokazuje, z czego składa się system i dlaczego wybraliśmy takie rozwiązania. Diagramy są w notacji C4 [1] i powstają z biblioteki C4-PlantUML [2], decyzje w formacie MADR [3].

| Strona | Co pokazuje |
| --- | --- |
| [Kontekst systemu](kontekst.md) | role i systemy zewnętrzne wokół systemu (C4, poziom 1) |
| [Kontenery](kontenery.md) | aplikację, serwer, bazy danych, Usługę logowania i połączenia między nimi (C4, poziom 2) |

## Decyzje architektoniczne

| ADR | Decyzja | Decyzje projektu |
| --- | --- | --- |
| [ADR-0001](decyzje/0001-stos-technologiczny.md) | Kotlin i Jetpack Compose, Spring Boot na Java 25, PostgreSQL z PostGIS | nr 2, 3, 28, 45 |
| [ADR-0002](decyzje/0002-monorepo.md) | jedno repozytorium dla kontraktu, serwera, aplikacji, danych i dokumentacji | nr 4, 5, 40 |
| [ADR-0003](decyzje/0003-keycloak.md) | konta, role i logowanie w Keycloak | nr 7 |
| [ADR-0004](decyzje/0004-gotowe-reguly-i-biblioteki.md) | tylko gotowe zestawy reguł jakości i dojrzałe biblioteki | nr 8, 22, 35, 41, 46 |
| [ADR-0005](decyzje/0005-mapa-offline.md) | mapa MapLibre bez sieci: kontury z OpenStreetMap, podkład tylko online | nr 14, 27 |
| [ADR-0006](decyzje/0006-dane-pwr-o-pomieszczeniach.md) | plany pięter PWr importuje serwer, w repozytorium ich nie ma | nr 15, 17, 44 |

ADR nie zmieniamy po przyjęciu. Nową decyzję zapisujemy kolejnym numerem ze statusem „zastępuje ADR-…”.

## Źródła

1. [Model C4](https://c4model.com/)
2. [C4-PlantUML — biblioteka diagramów C4](https://github.com/plantuml-stdlib/C4-PlantUML)
3. [MADR — Markdown Architectural Decision Records](https://adr.github.io/madr/)
