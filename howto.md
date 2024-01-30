
# Utkast på min side
En enklere inngang til påbegynte søknader og dokument-innsendinger.

Utkast er en underside av min side (tilgjengelig på `<navurl>/minside/utkast`) der bruker skal kunne finne søknader og dokument-innsendinger hen har begynt å fylle ut, men ikke sendt inn enda.<br>

* Utkast er bruker-initiert
* Det kan være innsendinger eller søknader som en person har startet på, men ikke sendt inn.
* Et utkast er _i øyeblikket ikke en del av en persons interaksjon med NAV_, selv om det kan bli det.
* Eksempel:  En påbegynt søknad. Den kan sendes inn, eller en bruker kan velge å ikke sende den inn.

## Hvordan fungerer det?
Koble på [min-side-utkast-iac-topicet](https://github.com/navikt/min-side-utkast-topic-iac) og kjør på!
_utkast støtter tre hendelser: created, updated, og deleted_

1. Send en created-melding når en bruker har lagret et utkast
2. Send en updated-melding hvis url-en eller tittelen til utkastet har endret seg.
3. Send en deleted melding når bruker enten har sendt inn, eller hvis utkastet har blitt slettet enten av bruker eller systemet.

## Kafka

**topic**: aapen-utkast-v1
**tilgang**: [min-side-utkast-iac-topic](https://github.com/navikt/min-side-utkast-topic-iac)
**hendelser**: `created`, `updated`, `deleted`

### meldingsformat

#### create og update

```json
{
  "@event_name": "<operasjon>",
  "utkastId": "<uuid>",
  "ident": "<fnr eller lignende>",
  "link": "<link til utkast>",
  "tittel": "<tittel på utkast>",
  "tittel_i18n": {
    "<språkkode>": "oversatt tittel"
  },
  "metrics": {
    "skjemanavn": "<skjema navn>",
    "skjemakode": "<NAV skjemakode>"
  }
}
```

**obligatoriske felt**: `@event_name`, `utkastId`, `"ident"`,`"tittel"`

#### delete

```json
{
  "@event_name": "deleted",
  "utkastId": "<uuid>"
}
```
Det finnes også et [message-builder bibliotek](https://github.com/navikt/tms-utkast/pkgs/container/tms-utkast%2Ftms-utkast) som lager meldingene for deg.
NB! Det er kun ting som **ikke er innsendt** som skal ligge på utkast-siden.

## Observability
Hendelser blir logget til kibana med customfelter for filtrering
Alle utkast: `x_contenttype:"utkast"`
Spesifikt utkast: `x_minside_id :"<utkastid>"` 

## Amplitude målinger

[Utkast-frontenden](https://github.com/navikt/tms-utkast-mikrofrontend) logger et [skjema åpent](https://github.com/navikt/analytics-taxonomy/tree/main/events/skjema%20%C3%A5pnet)-event med `url` som payload til amplitude når bruker klikker på ett utkast. Om teamet ditt ønsker å få med feltene `skjemakode` og `skjemanavn` må metricsfeltet være tilstede og komplett i create-meldingen

