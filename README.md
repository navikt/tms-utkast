# tms-utkast

Backend for utkast-funksjonalitet på min side.

## Dokumentasjon

Dokumentasjon for produsenter finnes i [how-to](/howto.md) og i dokumnetasjonsidene til min side

### Oppdatere produsent-dokumentasjon

1. Oppdater howto.md. 
   **NB!** Overskriftshierarkiet skal starte på `h1`/`# ` og beholde riktig sekvensiell struktur i
   hele dokumentet (`##`,deretter `###` osv). Linebreaks må være på markdown format `<noe som skal følges av ett linebreak> /\`
2. Commit som inneholder endringer i howto-fil trigger automatisk en rebuild av [team min sides dokumentasjonen](https://github.com/navikt/tms-dokumentasjon)

## Rapid

**topic**: aapen-utkast-v1 \
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

