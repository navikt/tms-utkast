# tms-utkast

Backend for utkast-funksjonalitet på min side.

## Rapid

**topic**: aapen-utkast-v1 <br>
**tilgang**: [min-side-utkast-iac-topic](https://github.com/navikt/min-side-utkast-topic-iac)<br>
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

### delete

```json
{
  "@event_name": "deleted",
  "utkastId": "<uuid>"
}
```

# Amplitude målinger

[Utkast-frontenden](https://github.com/navikt/tms-utkast-mikrofrontend) logger
ett [skjema åpent](https://github.com/navikt/analytics-taxonomy/tree/main/events/skjema%20%C3%A5pnet) 
event med `url` som payload til amplitude når bruker klikker på ett utkast. Om teamet ditt ønsker å få med feltene 
`skjemakode` og `skjemanavn` må metricsfeltet være tilstede og komplett i create-meldingen

