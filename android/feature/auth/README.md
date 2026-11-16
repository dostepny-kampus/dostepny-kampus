# :feature:auth

## Zadanie

Konto osoby: prośba o logowanie (L1).

## Pakiety

- `feature.auth` — stan i ViewModel prośby o logowanie.

## API

ViewModel prośby o logowanie w pakiecie `feature.auth`.

## Zależności

- `:core:data`

## Czego moduł nie robi

Nie rozmawia z Usługą logowania i nie przechowuje tokenu; logowanie w przeglądarce i sesja konta są w `:core:data`.

## Kiedy dodawać tutaj

Ekrany i stan konta: logowanie, rejestracja, role i wylogowanie. Token i sesja należą do `:core:data`.
