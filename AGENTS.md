# Zasady pracy w repozytorium

Ten plik mówi, jak prowadzić zmianę w Dostępnym Kampusie. Reguły, które sprawdza CI, są w
[CONTRIBUTING.md](CONTRIBUTING.md) razem z plikiem, który je egzekwuje; tu są wymienione tylko z nazwy.
Szczegółowo opisane jest to, czego CI nie sprawdzi. Plik czyta się w całości przy każdej zmianie, więc ma
najwyżej 150 linii (`.github/scripts/dlugosc-pliku-test.sh`).

## 1. Mapa repozytorium

| Katalog | Zawartość |
|---|---|
| `android/` | aplikacja: `app`, moduły `core/*` (dane, baza, sieć, model, geometria, lokalizacja, wygląd), moduły `feature/*` (ekrany) |
| `backend/` | serwer Spring Boot; moduły to podpakiety `io.github.dostepnykampus` |
| `api/openapi.yaml` | kontrakt HTTP między aplikacją a serwerem; zmiana kontraktu idzie przed zmianą kodu |
| `specs/` | scenariusze zachowania (`.feature`), część wykonywana jako testy |
| `docs/` | dokumentacja: potrzeby, wymagania, UX, architektura; buduje się w trybie ścisłym |
| `data/` | dane budynków i skrypty, które je przygotowują |
| `.github/` | workflow CI, skrypty kontroli i ich testy |

Opis modułu leży przy kodzie: w `package-info.java` modułu serwera i w `README.md` modułu Androida
(wprowadzane w #1333 i #1334). Zanim zmienisz moduł, przeczytaj jego opis.

## 2. Kolejność pracy

1. Znajdź albo załóż issue. Przypisz się w chwili, gdy zaczynasz, nie wcześniej.
2. Gałąź `N-krotki-opis` od aktualnej gałęzi `sprint/N`.
3. Jedno issue to jeden PR, możliwie najmniejszy. 300 linii to sufit, nie cel.
4. Testy są pierwsze: najpierw test, który nie przechodzi, potem kod.
5. PR do `sprint/N` z `Closes #N`; po zielonym CI kolejka scalania.

Zadanie należące do epiku jest jego sub-issue, a opis issue i PR zaczyna się od `Część #N`.
Jedno issue ma najwyżej 100 sub-issue; pełny epik dostaje następcę.

## 3. Zmiana nie mieści się w jednym PR

- Najpierw osobny PR z samymi testami; nowe przypadki są wyłączone przez `@Disabled`/`@Ignore` z powodem
  i numerem issue. PR z kodem zdejmuje wyłączenia w osobnym commicie, przed kodem.
- Jeśli to nie wystarcza, zostaw w kodzie `TODO(#N)` i załóż issue z ciągiem dalszym. Kto zostawił
  `TODO(#N)`, ten kończy to issue.
- Nie osłabiaj oczekiwań testu po to, żeby kod przeszedł. Spór o wymaganie rozstrzyga właściciel.

## 4. Najpierw porządek, potem funkcja

Zanim dodasz funkcję, obejrzyj kod, w który trafi: zależności między modułami, spójność klasy, poziomy
abstrakcji, własne rozwiązanie w miejscu sprawdzonej biblioteki. Jeśli nowy kod musiałby pogorszyć którąś
z tych rzeczy:

1. załóż issue na poprawkę (jeśli go nie ma),
2. popraw kod w osobnym PR,
3. dopiero potem dodaj funkcję.

Czerwonej kontroli nie ucisza się ściskaniem kodu ani wyjątkiem, tylko podziałem według odpowiedzialności.

## 5. Znany problem to issue od razu

Błąd, zły projekt kodu, dziura w CI, rozjazd kodu z dokumentacją, test, który raz padł i po ponowieniu
przeszedł: wszystko trafia do Issues w chwili zauważenia, pod właściwy epik. Najpierw poszukaj
istniejącego issue i dopisz tam wystąpienie. Kod niepodłączony albo odłożony dostaje `TODO(#N)` przed
pierwszym pushem.

Decyzja zapisana w rozmowie albo w dokumencie od razu staje się issue; numery wracają do miejsca decyzji.

## 6. Gdzie kłaść kod i kiedy zakładać moduł

Moduł działa jak mała usługa: jedno zadanie, jawne API, jawne zależności.

- Serwer: korzeń pakietu modułu zawiera tylko typy wołane przez inne moduły. Reszta leży w podpakietach
  `domain`, `web`, `persistence` albo w podpakiecie tematycznym, gdy temat ma co najmniej pięć plików.
- Android: granicę modułu wyznaczają zależności Gradle i widoczność `internal`. Moduł funkcji nie sięga
  do sieci ani do bazy; robi to przez `core/data`.
- Nowa zależność między modułami to świadoma decyzja, widoczna w PR, nie skutek uboczny importu.

Nowy moduł zakładasz, gdy naraz: ma kod już teraz; jego zadanie da się opisać jednym zdaniem bez „i”;
ma własne dane albo własną ścieżkę użytkownika; jego API to kilka typów. Pakiet powyżej 15 plików dziel.
Moduł poniżej trzech plików z jednym odbiorcą połącz z odbiorcą. Pustych modułów „na przyszłość” nie ma.

Stan docelowy i kolejność prac: epik #1335.

## 7. Opisy w kodzie

Javadoc i KDoc opisują tylko kontrakt elementu: co robi, co znaczą parametry i `null`, jakie błędy
i skutki uboczne. Komentarzy wewnątrz kodu nie ma.

Opis nie zawiera:

- wyglądu ekranu, tekstów przycisków, zachowania czytnika ekranu (to jest w wymaganiach i makietach),
- zachowania innych elementów ani kodu wywołującego,
- opisu testów,
- historii zmian, numerów issue ani numerów decyzji; wyjątkiem jest `TODO(#N)`.

Identyfikatory wymagań stoją w jednej linii na końcu opisu: `Wymagania: US1.1, NFR-19`.
Powód zmiany jest w opisie PR i w issue. Opis modułu zmienia się w tym samym PR co moduł.
Przykład „przed” i „po” oraz kontrole każdego punktu: `docs/docs/architektura/opisy-w-kodzie.md`.
Reguły i czyszczenie istniejących opisów: epik #1336.

## 8. Testy uruchamia CI

Testów, analizy statycznej i pełnej budowy nie uruchamia się lokalnie. Wypchnij gałąź, otwórz PR
i odczytaj wynik z zadań CI dla aktualnego commita. Podczas przebiegu CI weź następne, niezależne zadanie.

Lokalnie wolno sprawdzić, czy kod się kompiluje:

```sh
cd backend && ./mvnw -q -DskipTests test-compile
cd android && ./gradlew compileDebugUnitTestKotlin compileTestKotlin
```

oraz uruchomić test zmienianego skryptu kontroli, np. `bash .github/scripts/rozmiar-pr-test.sh`.
Lokalny test aplikacji albo serwera jest dopuszczalny tylko wtedy, gdy CI nie da potrzebnego dowodu;
powód zapisz w opisie PR.

Po zmianie interfejsu obejrzyj prawdziwy zrzut ekranu wyniku, nie tylko wynik testu.
Po scaleniu sprawdź CI gałęzi `sprint/N`.

## 9. Kontroli nie wolno osłabiać

- Nie dodawaj wyciszeń analizy, wyjątków od reguł ani niższych progów, żeby zmiana przeszła.
- Lista wyjątków reguły może tylko maleć; nowy wpis wymaga issue i zgody właściciela.
- Zmiana workflow, konfiguracji linterów albo plików reguł jakości wymaga w opisie PR linii
  `Zmiana reguł jakości: <powód>` i zatwierdzenia etykietą przez właściciela.
- Rozwiązanie tymczasowe musi samo się zgłaszać: adnotacja z numerem otwartego issue i test, który
  pilnuje jej użycia.

Co CI sprawdza samo (szczegóły w [CONTRIBUTING.md](CONTRIBUTING.md)): powiązanie PR z issue, rozmiar PR,
postać commitów, nagłówki plików, zakazane słowa, sekrety i licencje zależności, znaczniki `TODO(#N)`,
granice modułów, pokrycie testami, zgodność serwera i aplikacji z kontraktem API.
