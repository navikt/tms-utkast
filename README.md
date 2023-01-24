# tms-utkast

Backend for utkast-funksjonalitet på min side.

## Dokumentasjon
Dokumentasjon for produsenter finnes i [how-to](/howto.md) og i dokumnetasjonsidene til min side

### Oppdatere produsent-dokumentasjon
1. Oppdater howto.md. **NB!** Overskriftshierarkiet skal starte på `h2` 
2. Bygg og deloy tms-dokumentasjonen på nytt. 

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

