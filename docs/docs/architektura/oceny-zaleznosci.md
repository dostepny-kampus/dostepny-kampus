# Oceny zależności

Oceny uzupełniają [zasady dodawania bibliotek](decyzje/0004-gotowe-reguly-i-biblioteki.md).
Wynik przeglądu dotyczy wskazanej wersji i dnia sprawdzenia.

## OkHttp 5.5.1

Zadanie: [#477](https://github.com/dostepny-kampus/dostepny-kampus/issues/477).

Data sprawdzenia: 13 listopada 2026. Decyzja: zostają OkHttp i MockWebServer w wersji `5.5.1`.

| Kryterium | Wynik |
| --- | --- |
| Dokładna wersja | `okhttp = "5.5.1"` w katalogu wersji Androida; ta sama wersja dla `okhttp` i `mockwebserver`. |
| Licencja | Apache-2.0, dozwolona przez ADR-0004. Potwierdzają ją [licencja wydania](https://github.com/lysine-dev/okhttp/blob/parent-5.5.1/LICENSE.txt) i [metadane Maven Central](https://repo.maven.apache.org/maven2/com/squareup/okhttp3/okhttp/5.5.1/okhttp-5.5.1.pom). |
| Aktualność | Wydanie z 22.10.2026, więc mieści się w ostatnich 12 miesiącach; [historia wydania](https://github.com/lysine-dev/okhttp/blob/parent-5.5.1/CHANGELOG.md#version-551). |
| Potrzeba klienta | `core:network` wysyła żądania API, ustawia token, limity czasu i przerywa połączenie przy anulowaniu korutyny. Usunięcie klienta wymagałoby innej biblioteki albo własnej obsługi tych czynności. |
| Potrzeba w mapie | MapLibre przyjmuje `okhttp3.Call.Factory`. `MapTileHttp` dodaje nagłówek User-Agent i dzieli pulę połączeń z klientem aplikacji. Zależność jest używana wprost, więc pozostaje jawna. |
| Potrzeba w testach | MockWebServer sprawdza żądania, kody odpowiedzi, przerwane połączenia, opóźnione odpowiedzi i anulowanie w `core:network` oraz `core:data`. |

Wersja `5.5.1` blokuje osiem skompromitowanych certyfikatów pośrednich eMudhra.
Według [opisu poprawki](https://github.com/lysine-dev/okhttp/blob/parent-5.5.1/CHANGELOG.md#version-551)
chroni to także urządzenia AOSP bez aktualizacji z Google Play Services.
Jest to dodatkowy powód utrzymania tego wydania.

### Znane podatności

Zapytania do [OSV](https://google.github.io/osv.dev/post-v1-query/) wykonano 13.11.2026.
Dla każdego pakietu podano ekosystem `Maven` i dokładną wersję `5.5.1`.

| Pakiet | Liczba znalezionych wpisów |
| --- | --- |
| `com.squareup.okhttp3:okhttp` | 0 |
| `com.squareup.okhttp3:okhttp-android` | 0 |
| `com.squareup.okhttp3:okhttp-jvm` | 0 |
| `com.squareup.okhttp3:mockwebserver` | 0 |

[GitHub Advisory Database](https://api.github.com/advisories?ecosystem=maven&affects=com.squareup.okhttp3%3Aokhttp%405.5.1)
także zwróciła pustą listę dla `com.squareup.okhttp3:okhttp@5.5.1`.
Nie znaleziono publicznego wpisu obejmującego sprawdzone wersje w tych bazach na dzień oceny.
Aktualizacje i nowe zgłoszenia podatności nadal sprawdza Dependabot.

Przykład powtórzenia zapytania OSV; pozostałe pakiety z tabeli sprawdza się przez zmianę `name`:

```json
{
  "package": {
    "ecosystem": "Maven",
    "name": "com.squareup.okhttp3:okhttp"
  },
  "version": "5.5.1"
}
```

Treść należy wysłać metodą POST do `https://api.osv.dev/v1/query`.
Ocena nie zmienia wersji ani reguł jakości. Zmiana klienta HTTP wymaga osobnego zadania i testów w CI.

## Coil 3.6.3

Zadanie: [#2107](https://github.com/dostepny-kampus/dostepny-kampus/issues/2107), ciąg dalszy w [#2015](https://github.com/dostepny-kampus/dostepny-kampus/issues/2015).

Data sprawdzenia: 15 listopada 2026. Decyzja: wersja `3.6.3` dla `coil-compose` i `coil-network-okhttp`.

| Kryterium | Wynik |
| --- | --- |
| Dokładna wersja | `coil = "3.6.3"` w katalogu wersji Androida; ta sama wersja dla obu artefaktów. To najnowsze stabilne wydanie w [Maven Central](https://repo1.maven.org/maven2/io/coil-kt/coil3/coil-compose/maven-metadata.xml). |
| Licencja | Apache-2.0, dozwolona przez ADR-0004. Potwierdzają ją [licencja wydania](https://github.com/coil-kt/coil/blob/3.6.3/LICENSE.txt) i [metadane Maven Central](https://repo1.maven.org/maven2/io/coil-kt/coil3/coil-network-okhttp-android/3.6.3/coil-network-okhttp-android-3.6.3.pom). |
| Aktualność | Wydanie z 18.09.2026, więc mieści się w ostatnich 12 miesiącach; [historia wydania](https://github.com/coil-kt/coil/blob/3.6.3/CHANGELOG.md). |
| Potrzeba | Zdjęcie zgłoszenia przyjętego trzeba pobrać z serwera i pokazać w karcie budynku i arkuszu bariery. Biblioteka daje pamięć podręczną, dobór rozmiaru i przerwanie pobierania razem z kompozycją, więc nie piszemy własnego pobierania bajtów i dekodowania (ADR-0004: gotowa biblioteka zamiast własnej infrastruktury). |
| Zgodność z OkHttp | `coil-network-okhttp` przyjmuje `okhttp3.Call.Factory`, więc korzysta z klienta aplikacji z `core:network` i jego puli połączeń. Wydanie deklaruje OkHttp `4.12.0`; Gradle zostawia wersję `5.5.1` z katalogu, a użyty interfejs `Call.Factory` jest w obu wersjach ten sam. Zgodność sprawdza CI przy pierwszym użyciu. |
| Użycie | `coil-core`, `coil-network-core` i `coil` (`SingletonImageLoader`; ta sama wersja) i `coil-network-okhttp` trafiają do `app`, który buduje jeden `ImageLoader` na kliencie z `core:network` (#2236). `coil-compose` i `coil-compose-core` trafiają do modułu, który pokazuje obraz (#2015); `coil-test` (`io.coil-kt.coil3:coil-test`, ta sama wersja) trafia tylko do testów tego modułu, żeby obrazy w testach nie szły przez sieć. |

### Znane podatności Coil

Zapytania do [OSV](https://google.github.io/osv.dev/post-v1-query/) wykonano 15.11.2026.
Dla każdego pakietu podano ekosystem `Maven` i dokładną wersję `3.6.3`.

| Pakiet | Liczba znalezionych wpisów |
| --- | --- |
| `io.coil-kt.coil3:coil-compose` | 0 |
| `io.coil-kt.coil3:coil-compose-android` | 0 |
| `io.coil-kt.coil3:coil-network-okhttp` | 0 |
| `io.coil-kt.coil3:coil-network-okhttp-android` | 0 |
| `io.coil-kt.coil3:coil-core` | 0 |
| `io.coil-kt.coil3:coil-network-core` | 0 |

[GitHub Advisory Database](https://api.github.com/advisories?ecosystem=maven&affects=io.coil-kt.coil3%3Acoil-compose%403.6.3)
także zwróciła pustą listę dla `coil-compose@3.6.3` i `coil-network-okhttp@3.6.3`.
Nie znaleziono publicznego wpisu obejmującego sprawdzone wersje w tych bazach na dzień oceny.
Aktualizacje i nowe zgłoszenia podatności nadal sprawdza Dependabot.
Zapytanie OSV powtarza się jak dla OkHttp, zmieniając `name` i `version`.

## Timber 5.0.1

Zadanie: [#2643](https://github.com/dostepny-kampus/dostepny-kampus/issues/2643).

Data sprawdzenia: 16 listopada 2026. Decyzja: Timber w wersji `5.0.1` w aplikacji, z wyjątkiem od kryterium aktualności z ADR-0004 (ADR-0013).

| Kryterium | Wynik |
| --- | --- |
| Dokładna wersja | `timber = "5.0.1"` w katalogu wersji Androida. |
| Licencja | Apache-2.0, dozwolona przez ADR-0004. Potwierdzają ją [licencja](https://github.com/JakeWharton/timber/blob/trunk/LICENSE.txt) i [metadane Maven Central](https://repo1.maven.org/maven2/com/jakewharton/timber/timber/5.0.1/timber-5.0.1.pom). |
| Aktualność | Wydanie z 13.08.2021, poza oknem 12 miesięcy. Maven Central nie ma nowszej wersji; biblioteka ma jedno zadanie (drzewa logów) i stabilny interfejs. |
| Potrzeba | Poziomy logów wybierane według typu budowy i tag z nazwy klasy bez własnej nakładki na `android.util.Log`. |
| Znane podatności | Zapytanie do [OSV](https://google.github.io/osv.dev/post-v1-query/) dla ekosystemu `Maven` i wersji `5.0.1` zwróciło 0 wpisów. [GitHub Advisory Database](https://api.github.com/advisories?ecosystem=maven&affects=com.jakewharton.timber%3Atimber%405.0.1) zwróciła pustą listę. |

## Macrobenchmark 1.5.0 i UI Automator 2.4.1

Zadanie: [#2668](https://github.com/dostepny-kampus/dostepny-kampus/issues/2668).

Data sprawdzenia: 16 listopada 2026. Decyzja: `benchmark-macro-junit4` `1.5.0` i `uiautomator` `2.4.1` tylko w module `:macrobenchmark`.

| Kryterium | Wynik |
| --- | --- |
| Dokładna wersja | `benchmark = "1.5.0"` i `uiautomator = "2.4.1"` w katalogu wersji Androida. |
| Licencja | Apache-2.0, dozwolona przez ADR-0004; potwierdzają ją metadane Maven Google obu pakietów. |
| Aktualność | Macrobenchmark `1.5.0` z 09.09.2026, UI Automator `2.4.1` z 04.11.2026, obie w ostatnich 12 miesiącach. |
| Potrzeba | Pomiar zimnego startu i czasu klatek na emulatorze w kontroli sprintu; moduł nie wchodzi do APK aplikacji. |
| Znane podatności | Zapytania do [OSV](https://google.github.io/osv.dev/post-v1-query/) dla ekosystemu `Maven` i podanych wersji zwróciły 0 wpisów. |

## JankStats 1.0.0

Zadanie: [#2666](https://github.com/dostepny-kampus/dostepny-kampus/issues/2666).

Data sprawdzenia: 16 listopada 2026. Decyzja: JankStats `1.0.0` tylko w wersji debug.

| Kryterium | Wynik |
| --- | --- |
| Dokładna wersja | `jankstats = "1.0.0"` w katalogu wersji Androida, zależność `debugImplementation`. |
| Licencja | Apache-2.0, dozwolona przez ADR-0004; potwierdzają ją [metadane Maven Google](https://dl.google.com/android/maven2/androidx/metrics/metrics-performance/1.0.0/metrics-performance-1.0.0.pom). |
| Aktualność | Wydanie z 08.10.2025, w ostatnich 12 miesiącach. |
| Potrzeba | Wolne klatki z etykietami stanu (ekran, działanie na mapie) w logcat obok logów aplikacji. |
| Znane podatności | Zapytanie do [OSV](https://google.github.io/osv.dev/post-v1-query/) dla ekosystemu `Maven` i wersji `1.0.0` zwróciło 0 wpisów. |

## LeakCanary 3.0-alpha-9

Zadanie: [#2666](https://github.com/dostepny-kampus/dostepny-kampus/issues/2666).

Data sprawdzenia: 16 listopada 2026. Decyzja: LeakCanary `3.0-alpha-9` tylko w wersji debug.

| Kryterium | Wynik |
| --- | --- |
| Dokładna wersja | `leakcanary = "3.0-alpha-9"` w katalogu wersji Androida, zależność `debugImplementation`. |
| Licencja | Apache-2.0, dozwolona przez ADR-0004; potwierdza ją [licencja](https://github.com/square/leakcanary/blob/main/LICENSE.txt). |
| Aktualność | Wydanie z 24.06.2026, w ostatnich 12 miesiącach. Wersja stabilna `2.14` jest z kwietnia 2024, poza oknem 12 miesięcy. |
| Potrzeba | Wykrycie wycieków pamięci w wersji debug bez wysyłania danych poza telefon (NFR-28). |
| Ryzyko wersji alfa | Narzędzie działa tylko w wersji debug i nie trafia do APK release. |
| Znane podatności | Zapytanie do [OSV](https://google.github.io/osv.dev/post-v1-query/) dla ekosystemu `Maven` i wersji `3.0-alpha-9` zwróciło 0 wpisów. |
