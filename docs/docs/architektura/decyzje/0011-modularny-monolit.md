# ADR-0011: Modularny monolit — pakiety według tematów, wejście przez `api`

Status: przyjęta. Data: 2026-11-14. Źródło: epik #1660. Zmienia układ wnętrza modułu ze standardu #1319.

## Kontekst i problem

Standard modułów ustalił granice między modułami, ale nie wnętrze modułu.

- Pakiety mieszają tematy, choć mieszczą się w limicie 15 plików: `core.model` (pięć tematów
  w 12 plikach), `core.database`, `core.geo`, korzeń `app`. Limit liczby plików nie wykrywa mieszania.
- Podział „najpierw warstwa” rozrzuca jeden temat po czterech pakietach (`domain`, `web`,
  `persistence`, `json`); `barrier.domain` ma 22 klasy dwóch tematów.
- API modułu to „typy w korzeniu pakietu” — umowa, której nie widać w drzewie katalogów.
- Ten sam temat ma różne nazwy w modułach (`building`, `buildings`), więc nie da się go znaleźć jednym
  wyszukiwaniem.
- Usługa zdjęć zależy od klienta S3; tabela zgłoszeń ma klucz obcy do tabeli budynków innego modułu.
- Nic nie sprawdza, że moduł serwera uruchamia się bez pozostałych.

## Rozważone opcje

1. Zostawić standard i obniżyć limit plików do 8. Limit nie wykrywa mieszania tematów, a niski
   wymusza sztuczny podział: dziesięć pakietów Androida ma ponad 8 plików, ekran Compose łatwo więcej.
2. Mikroserwisy. Trzy osoby i jedno wdrożenie; osobne skalowanie nie jest potrzebne.
3. Moduły serwera jako moduły Mavena, granicę pilnuje kompilator. Spring Modulith sprawdza te same
   granice testem; do opcji wracamy, gdy test przepuści naruszenie, które zatrzymałby kompilator.
4. Modularny monolit na Spring Modulith; wejście do modułu przez pakiet `api`; wnętrze według tematów;
   reguły z bibliotek: Spring Modulith, ArchUnit, Konsist, Module Graph Assert.

## Decyzja

Opcja 4. Pakiety dzielimy według zasad komponentów [6]:

- wspólne domknięcie: klasy zmieniane razem leżą razem, pakiet ma jeden temat;
- wspólne użycie: kto używa jednej klasy pakietu, zwykle potrzebuje pozostałych;
- brak cykli między pakietami i modułami;
- stabilne zależności: adapter zależy od dziedziny, dziedzina od adaptera nie.

Z tego wynika:

- Moduł z jednym tematem ma warstwy pod korzeniem; moduł z kilkoma tematami ma tematy, a w nich warstwy.
- Inny moduł widzi tylko pakiet `api` (`@NamedInterface`). Agregaty, encje i repozytoria nie wychodzą
  poza moduł. Interfejs powstaje tylko w `api` albo jako port dziedziny.
- Nazwy tematów pochodzą z jednego słownika i są takie same w każdym module.
- Moduł jest właścicielem swoich tabel; migracje modułu w jego katalogu; nowych kluczy obcych między
  modułami nie ma.
- Każdy moduł serwera ma test uruchamiający tylko jego kontekst.
- Liczba plików w pakiecie (15) jest sygnałem w CI, nie zasadą podziału.
- Zdarzenia między modułami wprowadzamy z pierwszym przypadkiem, razem z rejestrem publikacji
  i odbiorcą odpornym na powtórzenie.

Zasady szczegółowe, układy katalogów i kontrole są w [Struktura modułów](../moduly.md).

## Konsekwencje

- Dobrze: temat leży w jednym katalogu i ma tę samą nazwę w każdym module. Kod znajduje się po
  nazwie tematu, a opis pakietu zastępuje czytanie plików.
- Dobrze: kontrole z bibliotek zamiast własnych; moduł serwera uruchamiany osobno w testach.
- Gorzej: około 30 PR przeniesień i konflikty z równoległą pracą; zmiana nazw pakietów Androida dotyka
  importów i modułów Hilt.
- Gorzej: test na moduł to osobny kontekst Springa z kontenerami, więc CI trwa dłużej.
- Gorzej: klucz obcy zgłoszenia do budynku zostaje wyjątkiem, dopóki import budynków nie publikuje
  zdarzenia.

## Źródła

1. [Spring Modulith — dokumentacja](https://docs.spring.io/spring-modulith/reference/)
2. [ArchUnit — podręcznik](https://www.archunit.org/userguide/html/000_Index.html)
3. [Konsist — dokumentacja](https://docs.konsist.lemonappdev.com/)
4. [Module Graph Assert](https://github.com/jraska/modules-graph-assert)
5. [Android — modularyzacja aplikacji](https://developer.android.com/topic/modularization)
6. R. C. Martin, „Clean Architecture”, część IV: zasady komponentów.
