# Verze ALTO v Komplexním validátoru

Pracovní přehled, stav k 15. 11. 2026 (verze 3.0, ve vývoji); text se bude měnit. Odkazuje na něj [README.md](README.md).

## Jak validátor ALTO kontroluje

Každá fDMF (`modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/fDMF/<dmf>/`) má
v adresáři `xsd/` jedno nebo více schémat `alto_<verze>.xsd`. Pravidlo pro soubory ALTO vybírá schéma podle
namespace kořenového elementu (`checkXmlIsValidByXsdByNamespace`): `ns-v2#` → 2.x, `ns-v3#` → 3.x, `ns-v4#` → 4.x.
Pro 3.x a 4.x je uloženo schéma nejvyšší minor verze (změny v rámci majoru jsou aditivní). Pro 2.x jsou uložena
obě schémata, `alto_2.0.xsd` i `alto_2.1.xsd`, protože 2.1 není nadmnožinou 2.0: ve 2.1 je element `MeasurementUnit`
v `Description` povinný, ve 2.0 nepovinný, a naopak 2.0 nezná prvky přidané ve 2.1 (`Tags`, `TAGREFS`, `LANG`).
Schémata se stejným `targetNamespace` jsou alternativy: soubor `ns-v2#` musí být validní aspoň proti jednomu z nich,
při neúspěchu hláška uvede chyby proti oběma (od verze 3.0; do 2.9 měla fDMF pro 2.x jen jedno schéma). Soubor
s namespace, pro který fDMF schéma nemá, je chyba „nepodporovaná verze formátu“. Pravidlo `…_SAME_VERSION` hlásí
WARNING, když balíček míchá verze. fDMF s jediným schématem používají starší pravidlo `checkXmlIsValidByXsd`; chování
je stejné jako dřív.

Zdroj schémat: https://github.com/altoxml/schema (adresáře v2, v3, v4); import
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
| Monografie 2.2 | 2.0 a 4.4; kap. 7.8 „verze aktuální v době vydání“; předpis OCR 1.0 | 2.0, 2.1, 3.1, 4.4 | podporováno |
| Monografie 2.3 | 2.0 a 4.4; kap. 7.9 předpis OCR 1.0 | 2.0, 2.1, 3.1, 4.4 | podporováno |
| Periodika 1.5 až 2.0 | 2.0 | 2.0 | souhlasí |
| Periodika 2.1 | 2.0 a 4.4; kap. 7.8 „aktuální v době vydání“; předpis OCR 1.0 | 2.0, 2.1, 3.1, 4.4 | podporováno |
| Periodika 2.2 | 2.0 a 4.4; kap. 7.9 předpis OCR 1.0 | 2.0, 2.1, 3.1, 4.4 | podporováno |
| Skládaná e-periodika 1.3, varianta B (skenované články) | 2.0 a 4.4 v tab. 1.4; kap. 5.8 předpis OCR 1.0 | 2.0, 2.1, 3.1, 4.4 | podporováno |
| Zvuk 0.3, 0.4, 0.5 (gramodesky), 0.3 (fonoválečky) | 3.1 v tab. 1.4; kap. 7.7 „poslední verze aktuální v době implementace, nebo verze předchozí (prosinec 2010 verze 2)“ | 2.0, 2.1, 3.1 | podporováno |
| Zvuk 1.0 (audio_gram, audio_fono, audio_disc, audio_no_carrier) | 3.1 a 4.4; kap. 7.9 předpis OCR 1.0 | 2.0, 2.1, 3.1, 4.4 | podporováno |
| Datové disky 0.1 | 2.0; kap. 7.8 „poslední verze aktuální v době implementace nebo verze předchozí“ | 2.0, 2.1, 3.1, 4.4 | podporováno |
| Jednotky fondu 0.1, 0.2 | 2.0 a 4.4; kap. 7.8 (0.1) a 7.9 (0.2) „aktuální v době vydání“, předpis OCR 1.0 | 2.0, 2.1, 3.1, 4.4 (obě verze stejně) | podporováno |

Žádná DMF nepřipouští jen ALTO 2.1 bez 2.0, proto mají obě schémata 2.x všechny fDMF, které vybírají schéma podle
namespace. fDMF se starými DMF (Monografie do 2.1, Periodika do 2.0) mají jen `alto_2.0.xsd` a pravidlo s jedním
schématem (`checkXmlIsValidByXsd`).

OCR (adresáře `txt` a `alto`) je ve zvuku podle DMF povinné jen u gramodesek 0.3 (DMF Zvuk 0.3, tabulka v kap. 5:
„ANO“). V gramodeskách 0.4 a 0.5 (tabulka v kap. 5 „NE“, kap. 5.7 a 5.8 „Tvoření je nepovinné“), ve fonoválečcích
(DMF 0.2, kap. 5.7 a 5.8 „Plnění je volitelné“) a v DMF Zvuk 1.0 (kap. 2 body 8 a 9, tabulka v kap. 5 „NE“, kap. 7.9
„Plnění OCR je dobrovolné“) je nepovinné; fDMF `audio_gram_0.4`, `audio_gram_0.5`, `audio_fono_0.3` a čtyři fDMF 1.0
adresáře nevyžadují, ale když existují, platí pro ně všechna pravidla (od verze 3.0).

DMF starší než Monografie 1.1 a Periodika 1.5 verzi ALTO neuvádějí. Oficiální vzorové balíčky NK (monografie 2.2,
periodikum 2.1, vícesvazková monografie 2.2) mají ALTO 2.0.

## Plán

Ve 3.0 vyřešeno: soubor ALTO 2.0 bez `MeasurementUnit` projde u všech fDMF s výběrem schématu podle namespace (do 2.9
ho fDMF se schématem 2.1, tj. Monografie 2.3, Periodika 2.2, Zvuk, Datové disky a Jednotky fondu, hlásily jako nevalidní).

Otevřené otázky pro NK ČR (výčet přípustných verzí, požadavky na obsah po verzích, míchání verzí v balíčku) jsou
shrnuty v návrhu předpisu OCR předaném NK.
