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
