Tento adresář obsahuje Xml Schémata pro validaci komponent stamotných PSP balíků, zejména popisných a strukturálních metadat.
ALTO: alto_2.0.xsd a alto_2.1.xsd (namespace ns-v2#), alto_3.1.xsd (ns-v3#) a alto_4.4.xsd (ns-v4#).
DMF Jednotky fondu 0.2 uvádí ALTO 2.0 a 4.4, kap. 7.9 odkazuje na předpis OCR (ALTO XML a TXT OCR) 1.0, který
připouští ALTO 2.0 a novější.
Schéma se vybírá podle namespace kořenového elementu (checkXmlIsValidByXsdByNamespace). Obě schémata 2.x se stejným
targetNamespace jsou alternativy: soubor ALTO 2.x musí být validní aspoň proti jednomu z nich. Proti 2.1 samotnému by
neprošel soubor ALTO 2.0 bez elementu MeasurementUnit (ve 2.1 je povinný, ve 2.0 ne), proti 2.0 samotnému soubor
s prvky přidanými ve 2.1 (Tags, TAGREFS, LANG). Pro 3.x a 4.x je tu nejvyšší minor verze (2.2 nikdy nevyšla, byla
přejmenována na 3.0).
Zdroj: https://github.com/altoxml/schema (adresáře v2, v3, v4). Import xlink.xsd je upraven na relativní lokální
soubor, aby validace nesahala na síť.
