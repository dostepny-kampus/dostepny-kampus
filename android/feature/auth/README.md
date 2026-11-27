# :feature:auth

## Zadanie

Konto osoby: prośba o logowanie (L1) i konto zalogowanej osoby z nazwą, rolami, klauzulą informacyjną, wycofaniem zgody i wylogowaniem (L2).

L1 i L2 zachowują wspólną szerokość tekstu i czynności w szerokim oknie.

## Pakiety

- `feature.auth` — ekran L1, jego stan i ViewModel; ekran L2 i czynności ról.

## API

`SignInScreen` z `SignInActions` (wejście ze „Zgłoś barierę” albo z „Konto”) i `SignInContent` bez modelu widoku.
`AccountScreen` z `AccountActions`; `WorkAction` to czynność roli, którą aplikacja zamienia na przejście.

## Zależności

- `:core:data`
- `:core:designsystem`
- `:core:model`

## Czego moduł nie robi

Nie rozmawia z Usługą logowania i nie przechowuje tokenu; logowanie w przeglądarce i sesja konta są w `:core:data`.

## Kiedy dodawać tutaj

Ekrany i stan konta: logowanie, rejestracja, role i wylogowanie. Token i sesja należą do `:core:data`.
