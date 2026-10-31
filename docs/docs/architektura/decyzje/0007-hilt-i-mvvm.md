# ADR-0007: Hilt i MVVM w aplikacji

Status: przyjęta. Data: 2026-10-31. Źródło: decyzje projektu nr 97 i 100.

## Kontekst i problem

Ekrany aplikacji czytają dane z kilku źródeł: katalog budynków z pliku w aplikacji, stan internetu, ustawienia w pliku DataStore. W Sprincie 2 ekrany tworzyły te źródła same, przez funkcje od `Context`, a stan ekranu był w funkcjach Compose. Przez to trudno było podmienić źródła w testach, a katalog trzeba było trzymać w ręcznym singletonie. Stan ekranu ginął razem z kompozycją.

## Rozważone opcje

- Wstrzykiwanie zależności: Hilt [1], Koin [2], ręczny kontener w klasie aplikacji.
- Stan ekranu: w funkcjach Compose, ViewModel z jednym stanem ekranu (MVVM) [3], biblioteka MVI.

## Decyzja

- Zależności tworzy Hilt. Źródła danych są w modułach Hilt w `core:data`, jedna instancja na proces.
- Każdy ekran z danymi ma ViewModel z `@HiltViewModel` w swoim module `feature:*` i jeden `StateFlow` ze stanem ekranu (`UiState`). Ekran wywołuje metody ViewModel, np. `chooseList`.
- ViewModel ekranu żyje tyle, co wpis ekranu w historii Navigation 3 [4]. Stan, który dotyczy całej aplikacji, np. czy to pierwsze uruchomienie, ma ViewModel na aktywność, też w module `feature:*` swojej historii użytkownika.
- Stan elementów interfejsu (widok kamery, tekst w polu, otwarty arkusz) zostaje w Compose, w `rememberSaveable`.
- Granice sprawdzają testy Konsist [5]: ViewModel tylko w `feature:*`, każdy z `@HiltViewModel`; moduły ekranów nie używają `core:network` ani `core:database` bezpośrednio.

## Konsekwencje

- Dobrze: Hilt sprawdza graf zależności przy każdej budowie [1]. Koin robi to tylko z adnotacjami i wtyczką kompilatora [2].
- Dobrze: ViewModel testujemy bez emulatora, na podróbkach źródeł.
- Dobrze: stan ekranu przetrwa obrót ekranu bez ponownego czytania źródeł.
- Gorzej: generowanie kodu (KSP) wydłuża budowanie, a testy z grafem Hilt potrzebują osobnej konfiguracji.
- Gorzej: ekran w teście bez grafu Hilt dostaje ViewModel podany ręcznie.

## Źródła

1. [Hilt: wstrzykiwanie zależności w Androidzie](https://developer.android.com/training/dependency-injection/hilt-android)
2. [Koin: opcje adnotacji i sprawdzanie grafu przy budowie](https://insert-koin.io/docs/reference/koin-annotations/options/)
3. [Android: warstwa interfejsu i stan ekranu](https://developer.android.com/topic/architecture/ui-layer)
4. [Navigation 3: ViewModel na wpis historii](https://developer.android.com/guide/navigation/navigation-3/save-state)
5. [Konsist](https://docs.konsist.lemonappdev.com/)
