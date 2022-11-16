# tms-utkast

Backend for utkast-funksjonalitet på min side. 

## Rapid
**topic**: aapen-utkast-v1 <br>
**tilgang**: [min-side-utkast-iac-topic](https://github.com/navikt/min-side-utkast-topic-iac)<br>
**operasjoner**: `created`, `updated`, `deleted`

### meldingsformat
#### create og update
```json
{
  "@event_name": "<operasjon>",
  "eventId": "<uuid>",
  "ident": "<fnr eller lignende>",
  "link": "<link til utkast>",
  "tittel": "<tittel på utkast>"
}
```
### delete
```json
{
  "@event_name": "deleted",
  "eventId": "<uuid>"
}
```


