# Fixtures Resolveru URN:NBN (reálné odpovědi API v5)

Soubory jsou odpovědi veřejného Resolveru (CZIDLO) stažené 2026-11-15 pomocí `curl` beze změny (jen čtení, bez účtu).
Validátor (`UrnNbnResolverChecker`) volá:

- stav: `<základ>/api/v5/urnnbn/<urn>?format=json` (soubory `*-urnnbn.json`)
- metadata digitálního dokumentu: `<základ>/api/v5/resolver/<urn>?format=json` (soubory `*-resolver.json`)

Základ je `https://resolver.nkp.cz` (předpona `prod-`) nebo `https://resolver-test.nkp.cz` (předpona `test-`).
Servíruje je atrapa v `UrnNbnResolverCheckerFixturesTest`. Stav v produkci se může měnit (deaktivace, oprava metadat);
fixtures jsou snímek ke dni stažení, testy proto nezávisí na síti.

| Soubor | URL (cesta za základem) | HTTP | Stav / typ | K čemu slouží |
|---|---|---|---|---|
| prod-mzk-006obk-urnnbn.json | /api/v5/urnnbn/urn:nbn:cz:mzk-006obk | 200 | ACTIVE | stav monografie |
| prod-mzk-006obk-resolver.json | /api/v5/resolver/urn:nbn:cz:mzk-006obk | 200 | monograph | titleInfo.title |
| prod-nk-003do9-urnnbn.json | /api/v5/urnnbn/urn:nbn:cz:nk-003do9 | 200 | ACTIVE | stav svazku monografie |
| prod-nk-003do9-resolver.json | /api/v5/resolver/urn:nbn:cz:nk-003do9 | 200 | monographVolume | monographTitle, volumeTitle |
| prod-abg001-0005lt-urnnbn.json | /api/v5/urnnbn/urn:nbn:cz:abg001-0005lt | 200 | ACTIVE | stav čísla periodika |
| prod-abg001-0005lt-resolver.json | /api/v5/resolver/urn:nbn:cz:abg001-0005lt | 200 | periodicalIssue | periodicalTitle, issueTitle, bez volumeTitle |
| prod-abg001-0005qv-urnnbn.json | /api/v5/urnnbn/urn:nbn:cz:abg001-0005qv | 200 | ACTIVE | stav čísla periodika (případ MLP) |
| prod-abg001-0005qv-resolver.json | /api/v5/resolver/urn:nbn:cz:abg001-0005qv | 200 | periodicalIssue | bez volumeTitle (B7) |
| prod-pna001-00cxz5-urnnbn.json | /api/v5/urnnbn/urn:nbn:cz:pna001-00cxz5 | 200 | ACTIVE | stav článku |
| prod-pna001-00cxz5-resolver.json | /api/v5/resolver/urn:nbn:cz:pna001-00cxz5 | 200 | analytical | článek, sourceDocument |
| prod-abg001-0003ig-urnnbn.json | /api/v5/urnnbn/urn:nbn:cz:abg001-0003ig | 200 | ACTIVE | stav entity OTHER |
| prod-abg001-0003ig-resolver.json | /api/v5/resolver/urn:nbn:cz:abg001-0003ig | 200 | otherEntity | documentType sheetmusic |
| prod-nk-00027d-urnnbn.json | /api/v5/urnnbn/urn:nbn:cz:nk-00027d | 200 | DEACTIVATED | deaktivovaný identifikátor |
| prod-nk-00027d-resolver.json | /api/v5/resolver/urn:nbn:cz:nk-00027d | 403 | error URN_NBN_DEACTIVATED | detail deaktivovaného |
| prod-nk-00026w-urnnbn.json | /api/v5/urnnbn/urn:nbn:cz:nk-00026w | 200 | DEACTIVATED | druhý deaktivovaný |
| prod-nk-00026w-resolver.json | /api/v5/resolver/urn:nbn:cz:nk-00026w | 403 | error URN_NBN_DEACTIVATED | |
| prod-nk-zzzzzz-urnnbn.json | /api/v5/urnnbn/urn:nbn:cz:nk-zzzzzz | 200 | FREE | nezaregistrovaný tvar (vymyšlený syntakticky platný kód) |
| prod-nk-zzzzzz-resolver.json | /api/v5/resolver/urn:nbn:cz:nk-zzzzzz | 404 | error UNKNOWN_DIGITAL_DOCUMENT | |
| prod-nk-00000e-urnnbn.json | /api/v5/urnnbn/urn:nbn:cz:nk-00000e | 200 | RESERVED | rezervované URN (první ze seznamu `/api/v5/registrars/nk/urnNbnReservations`) |
| prod-xx-urnnbn.json | /api/v5/urnnbn/urn:nbn:cz:xx | 400 | error INVALID_URN_NBN | chybná syntaxe |
| test-nk-00027d-urnnbn.json | /api/v5/urnnbn/urn:nbn:cz:nk-00027d (resolver-test) | 200 | ACTIVE | testovací instance (jiná data než produkce) |
| test-nk-00027d-resolver.json | /api/v5/resolver/urn:nbn:cz:nk-00027d (resolver-test) | 200 | periodicalIssue | s volumeTitle, na rozdíl od produkce |
| test-mzk-006obk-urnnbn.json | /api/v5/urnnbn/urn:nbn:cz:mzk-006obk (resolver-test) | 200 | FREE | stejné URN je v produkci ACTIVE, v testu FREE |

Poznámky:

- Testovací instance je čitelná bez účtu (GET). Zápis (registrace) by účet vyžadoval; nic se nezapisovalo.
- Odpověď pro chybný formát má HTTP 400 a tělo s `error`; atrapa vrací stejné kódy jako reálný server (tabulka).
- Nepodařilo se získat: entitu jednotky fondu (MLP, `UNIT`) v Resolveru, protože zatím není zaregistrována;
  odpověď pro `INTERNAL_ERROR` (nelze vyvolat); `/api/v6/` (validátor volá v5).
