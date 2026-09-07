Tento adresář obsahuje Xml Schémata pro validaci komponent stamotných PSP balíků, zejména popisných a strukturálních metadat.
ALTO: alto_2.0.xsd, alto_3.1.xsd a alto_4.4.xsd pokrývají verze ALTO 2.x, 3.x a 4.x (namespace ns-v2#, ns-v3#, ns-v4#),
které připouští předpis OCR (ALTO XML a TXT OCR) 1.0. Schéma se vybírá podle namespace kořenového elementu
(checkXmlIsValidByXsdByNamespace). V kopiích alto_2.0.xsd a alto_3.1.xsd je odstraněn import xlink.xsd z http://www.loc.gov,
zůstává jen relativní import lokálního xlink.xsd, aby validace nesahala na síť.
