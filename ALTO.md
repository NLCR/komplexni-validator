# Verze ALTO v Komplexním validátoru

Pracovní přehled, stav k 15. 10. 2026; text se bude měnit. Odkazuje na něj [README.md](README.md).

## Jak validátor ALTO kontroluje

Každá fDMF (`modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/fDMF/<dmf>/`) má
v adresáři `xsd/` jedno nebo více schémat `alto_<verze>.xsd`. Pravidlo pro soubory ALTO vybírá schéma podle
namespace kořenového elementu (`checkXmlIsValidByXsdByNamespace`): `ns-v2#` → 2.x, `ns-v3#` → 3.x, `ns-v4#` → 4.x.
Pro každý major je zpravidla uloženo schéma nejvyšší minor verze. Změny v rámci majoru jsou až na jednu výjimku
aditivní: ve 2.1 je element `MeasurementUnit` v `Description` povinný, ve 2.0 nepovinný. Soubor ALTO 2.0 bez
`MeasurementUnit` je proto validní proti `alto_2.0.xsd`, ale ne proti `alto_2.1.xsd` (známé omezení, viz Plán).
fDMF Monografie 2.2 a Periodika 2.1 mají proto pro 2.x schéma 2.0 jako dříve, ostatní fDMF s více schématy 2.1. Soubor s namespace, pro který fDMF schéma nemá, je chyba „nepodporovaná
verze formátu“. Pravidlo `…_SAME_VERSION` hlásí WARNING, když balíček míchá verze. fDMF s jediným schématem
používají starší pravidlo `checkXmlIsValidByXsd`; chování je stejné jako dřív.

Zdroj schémat: https://github.com/altoxml/schema (v2/alto-2-1.xsd, v3/alto-3-1.xsd, v4/alto-4-4.xsd); import
`xlink.xsd` je v kopiích přepsán na lokální soubor, aby validace nesahala na síť.

## Verze ALTO

| verze | rok | poznámka |
|---|---|---|
| 2.0 | 2010 | ALTO 1.4 převedené pod namespace Library of Congress (`ns-v2#`) |
| 2.1 | 2014 | aditivně `Tags`, `TAGREFS`, `LANG`; poslední 2.x (2.2 nevyšla, přejmenována na 3.0) |
| 3.0, 3.1 | 2014, 2016 | nový namespace `ns-v3#`, `SCHEMAVERSION`, shapes |
| 4.0 až 4.4 | 2018 až 2023 | namespace `ns-v4#`, glyphs, `Processing`, reading order; 4.4 je aktuální |

## Co požadují jednotlivé DMF a co mají fDMF

Verze v textu DMF jsou z tabulky 1.4 a z kapitoly OCR; od DMF Monografie 2.2 a Periodika 2.1 je závazný předpis
„OCR (ALTO XML a TXT OCR)“ 1.0 (17. 12. 2024), který připouští každou verzi ALTO od 2.0 a doporučuje aktuální.

| DMF | ALTO v textu DMF | XSD ve fDMF | důsledek |
|---|---|---|---|
| Monografie 1.1 až 2.1 | 2.0 | 2.0 | souhlasí |
| Monografie 2.2 | 2.0 a 4.4; kap. 7.8 „verze aktuální v době vydání“; předpis OCR 1.0 | 2.0, 3.1, 4.4 | podporováno |
| Monografie 2.3 | 2.0 a 4.4; kap. 7.9 předpis OCR 1.0 | 2.1, 3.1, 4.4 | podporováno |
| Periodika 1.5 až 2.0 | 2.0 | 2.0 | souhlasí |
| Periodika 2.1 | 2.0 a 4.4; kap. 7.8 „aktuální v době vydání“; předpis OCR 1.0 | 2.0, 3.1, 4.4 | podporováno |
| Periodika 2.2 | 2.0 a 4.4; kap. 7.9 předpis OCR 1.0 | 2.1, 3.1, 4.4 | podporováno |
| Zvuk 0.3, 0.4, 0.5 (gramodesky, fonoválečky) | 2.0 a 3.1 | 2.1, 3.1 | podporováno |
| Zvuk 1.0 (audio_gram, audio_fono, audio_disc, audio_no_carrier) | 3.1 a 4.4; kap. 7.9 předpis OCR 1.0 | 2.1, 3.1, 4.4 | podporováno |
| Datové disky 0.1 | 2.0 | 2.0 | souhlasí |
| Jednotky fondu 0.1 | 2.0 a 4.4; kap. 7.8 „aktuální v době vydání“ | 2.1, 3.1, 4.4 | podporováno |

DMF starší než Monografie 1.1 a Periodika 1.5 verzi ALTO neuvádějí. Oficiální vzorové balíčky NK (monografie 2.2,
periodikum 2.1, vícesvazková monografie 2.2) mají ALTO 2.0.

## Plán

Známé omezení: u fDMF se schématem `alto_2.1.xsd` (Monografie 2.3, Periodika 2.2, Zvuk, Jednotky fondu) neprojde
soubor ALTO 2.0 bez `MeasurementUnit`. Řešením je pro `ns-v2#` přijmout soubor validní proti 2.0 nebo 2.1; plánováno
v navazujícím rozvoji.

Otevřené otázky pro NK ČR (výčet přípustných verzí,
požadavky na obsah po verzích, míchání verzí v balíčku) jsou shrnuty v návrhu předpisu OCR předaném NK.
