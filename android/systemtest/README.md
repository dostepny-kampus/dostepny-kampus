# systemtest

## Odpowiedzialność

Sprawdza kontrakt klientów aplikacji z prawdziwym serwerem uruchomionym na gospodarzu emulatora.

## Publiczne API

Moduł nie udostępnia API aplikacji. Uruchamiacz AndroidJUnit4 wykonuje odczyt budynków oraz zapis
i odczyt własnego zgłoszenia z tokenem Zbierającego z realmu deweloperskiego.

## Własne dane

Tworzy identyfikatory zgłoszenia i zdjęcia dla pojedynczego przebiegu. Token pozostaje w pamięci;
moduł nie zapisuje go w pliku ani w logu. Baza i realm należą do środowiska CI.

## Zależności

Korzysta z `core:network` do żądań HTTP i `core:model` do treści oraz statusu zgłoszenia.
Testy wymagają serwera, bazy i Keycloak z `infra/docker-compose.yml`.

## Granice

Nie zastępuje sieci atrapą i nie wyłącza kontroli podpisu ani wystawcy tokenu na serwerze.
Nie sprawdza interfejsu ekranów ani fizycznego aparatu; wykonuje się w zestawie systemowym CI.
