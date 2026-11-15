Tento adresář obsahuje Xml Schémata pro validaci komponent stamotných PSP balíků, zejména popisných a strukturálních metadat.
ALTO: alto_2.0.xsd a alto_2.1.xsd (namespace ns-v2#), alto_3.1.xsd (ns-v3#).
DMF Zvuk 0.3 (gramodesky) uvádí v tab. 1.4 ALTO 3.1 a v kap. 7.7 „poslední verze formátu ALTO XML aktuální v době
implementace, nebo verze předchozí (prosinec 2010 verze 2)“; podle historie verzí v DMF Zvuk 1.0 se kapitola OCR ve
verzích 0.4, 0.5 a ve fonoválečcích 0.3 neměnila.
Schéma se vybírá podle namespace kořenového elementu (checkXmlIsValidByXsdByNamespace). Obě schémata 2.x se stejným
targetNamespace jsou alternativy: soubor ALTO 2.x musí být validní aspoň proti jednomu z nich. Proti 2.1 samotnému by
neprošel soubor ALTO 2.0 bez elementu MeasurementUnit (ve 2.1 je povinný, ve 2.0 ne), proti 2.0 samotnému soubor
s prvky přidanými ve 2.1 (Tags, TAGREFS, LANG). Pro 3.x a 4.x je tu nejvyšší minor verze (2.2 nikdy nevyšla, byla
přejmenována na 3.0).
Zdroj: https://github.com/altoxml/schema (adresáře v2, v3, v4). Import xlink.xsd je upraven na relativní lokální
soubor, aby validace nesahala na síť.
