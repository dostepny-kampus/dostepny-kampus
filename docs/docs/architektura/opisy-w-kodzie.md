# Opisy w kodzie

KDoc i Javadoc opisują tylko kontrakt elementu: to, co element gwarantuje wywołującemu. Zasada obejmuje
kod produkcyjny i testy, Kotlin i Javę. Komentarzy wewnątrz kodu nie ma.

## Co wchodzi do opisu

- co element robi, widziane od strony wywołującego;
- znaczenie parametrów, wyniku i wartości `null`;
- błędy, które element zgłasza;
- skutki uboczne: zapis, sieć, zmiana stanu;
- w teście: zachowanie, które test sprawdza.

## Czego w opisie nie ma

| Treść | Gdzie jest naprawdę |
| --- | --- |
| wygląd ekranu, teksty przycisków, kształty, kolory, zachowanie czytnika ekranu | wymagania ekranów i makiety, testy zrzutów |
| zachowanie innych elementów i kodu wywołującego | opisy tamtych elementów |
| opis testów, które sprawdzają element | same testy |
| historia zmian, numery issue i decyzji | opis PR i issue |

Wyjątkiem jest znacznik pracy odłożonej `TODO(#N)`: wskazuje otwarte issue i znika razem z nim.

Identyfikatory wymagań stoją w jednej linii na końcu części opisowej: `Wymagania: US1.1, NFR-19`.
Nie wplata się ich w zdania opisu.

## Przykład

Przed:

```kotlin
/**
 * Pole wyszukiwania kampusu w V1. Zielony przycisk „Szukaj” z ikoną lupy, TalkBack czyta
 * „Wyszukaj budynek” (US2, US2.3). Wcześniej był tu rozwijany wybór, zmieniony w decyzji 82
 * (#1127). Test zrzutu sprawdza oba motywy.
 */
```

Po:

```kotlin
/**
 * Pokazuje pole wyszukiwania i zgłasza wpisane zapytanie przez [onQuery].
 * Puste zapytanie nie wywołuje [onQuery].
 *
 * Wymagania: US2, US2.3
 */
```

Wygląd przycisku jest w wymaganiach ekranu i w teście zrzutu, a powód zmiany w PR, który ją wprowadził.

## Co pilnuje zasady

| Punkt zasady | Kontrola |
| --- | --- |
| bez numerów issue i decyzji w KDoc | reguła detekt `NoHistoryNumberInKDoc` (#1338) |
| część opisowa KDoc najwyżej 6 niepustych linii | reguła detekt `KDocDescriptionLength` (#1339) |
| identyfikatory wymagań tylko w linii `Wymagania:` i tylko istniejące | #1340 |
| odwołania `[Nazwa]` w KDoc wskazują istniejące elementy | #1341 |
| te same reguły dla Javadoc serwera | #1346 |
| treść kontraktu zamiast wyglądu i zachowania innych elementów | przegląd PR; długość opisu ogranicza #1339 |

Reguły mają listy wyjątków z dzisiejszymi opisami. Lista może tylko maleć; czyszczenie modułów
prowadzi epik #1336.
