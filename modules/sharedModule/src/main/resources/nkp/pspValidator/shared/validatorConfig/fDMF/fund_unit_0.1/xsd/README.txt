Tento adresář obsahuje Xml Schémata pro validaci komponent stamotných PSP balíků, zejména popisných a strukturálních metadat.
ALTO: alto_2.1.xsd, alto_3.1.xsd a alto_4.4.xsd pokrývají verze ALTO 2.x, 3.x a 4.x (namespace ns-v2#, ns-v3#, ns-v4#),
které připouští předpis OCR (ALTO XML a TXT OCR) 1.0. Schéma se vybírá podle namespace kořenového elementu
(checkXmlIsValidByXsdByNamespace); pro každý major je tu nejvyšší minor verze, protože změny v rámci majoru jsou
aditivní (dokument ALTO 2.0 je validní i proti 2.1, ověřeno na vzorku NK; 2.2 nikdy nevyšla, byla přejmenována na 3.0).
Zdroj: https://github.com/altoxml/schema (v2/alto-2-1.xsd, v3/alto-3-1.xsd, v4/alto-4-4.xsd). Import xlink.xsd je
upraven na relativní lokální soubor, aby validace nesahala na síť.
