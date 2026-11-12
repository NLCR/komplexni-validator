#!/usr/bin/env python3
"""
Generator syntetickeho SIP balicku elektronicke publikace (krok P5 plánu portu VEP):

  --type emo  e-monografie (DMF Elektronické publikace – monografie 2.3, 2.4, 3.0, 3.1)
  --type epe  e-periodika (DMF Elektronické publikace – periodika 2.3, 2.4, 2.5, 2.6)
  --type sep  skládaná e-periodika (DMF 1.3, varianta A: original/, amdsec/, nepovinná originaldata/)

Balicek je stavěn z tabulek DMF (kap. 3 struktura, 4 názvová konvence, 5 metadata), šablony MODS a DC vycházejí
z oficiálních vzorků NK (e-monografie 3.0 a e-periodika 2.5, EPUB) zkrácených na minimum a z profilů v
biblioProfiles; nečte hotovou fDMF, aby slouzil jako nezavisla kontrola pravidel. Cil: balicek bez ERROR proti
dane fDMF s vypnutymi externimi nastroji (veraPDF, EPUBCheck) a bez Resolveru.

Nazev cilového adresáre je PSP-ID:
  emo: druhá část URN:NBN svazku (např. aba007-p5m301), svazek dostane urn:nbn:cz:<PSP-ID>
  epe, sep: UUID čísla (ISSUE), tj. intelektuální entity

Pouziti:
    python3 gen_epub_package.py <cilovy_adresar_balicku> --type emo|epe|sep --version X.Y
        [--format pdf|epub] [--standard aacr|rda] [--chapters N] [--articles N] [--with-supplement]
        [--with-originaldata] [--mutation NAME]... [--pdf SOUBOR] [--epub SOUBOR] [--od SOUBOR]

Archivní kopie je skutečný soubor (--pdf, --epub; default malé soubory ze src/test/resources/epublikace), velikosti
a MD5 v METS, PREMIS, MD5 a INFO odpovídají obsahu. Mutace: --mutation NAME, seznam přes --list-mutations; každá je
jednoduchá změna vygenerovaných dat a popis říká, které pravidlo ji má odhalit (nebo proč má balíček zůstat validní).
Mutace, která by v dané kombinaci parametrů nic nezměnila, je odmítnuta.
"""
import argparse
import hashlib
import os
import re
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.normpath(os.path.join(HERE, "..", "resources", "epublikace"))
DEFAULT_PDF = os.path.join(RES, "dummy_PDFA-1B.pdf")
DEFAULT_EPUB = os.path.join(RES, "synteticky_epub3.epub")
DEFAULT_OD = os.path.join(RES, "synteticky_bez_pdfa.pdf")

CREATED = "2026-11-12T10:00:00"
CREATED_DAY = "2026-11-12"
SIGLA = "ABA007"

# podporované fDMF: (typ, verze) -> verze MODS, mets/@TYPE, originaldata povolena
FDMF = {
    ("emo", "2.3"): ("3.6", False), ("emo", "2.4"): ("3.6", False),
    ("emo", "3.0"): ("3.6", False), ("emo", "3.1"): ("3.8", True),
    ("epe", "2.3"): ("3.6", False), ("epe", "2.4"): ("3.6", False),
    ("epe", "2.5"): ("3.6", False), ("epe", "2.6"): ("3.8", True),
    ("sep", "1.3"): ("3.8", True),
}
METS_TYPE = {"emo": "electronic_monograph", "epe": "electronic_periodical", "sep": "electronic_coll_journal"}
# predchozi verze DMF pro mutaci info-metadataversion-old
PREVIOUS = {("emo", "2.4"): "2.3", ("emo", "3.0"): "2.4", ("emo", "3.1"): "3.0",
            ("epe", "2.4"): "2.3", ("epe", "2.5"): "2.4", ("epe", "2.6"): "2.5"}

# identifikátory (pevné, aby byly balíčky reprodukovatelné); PSP-ID je název cílového adresáře
VOLUME_UUID = "5e0b0001-0000-4000-8000-000000000001"   # emo: svazek; epe/sep: ročník
TITLE_UUID = "5e0b0002-0000-4000-8000-000000000002"    # epe/sep: titul
ISSUE_UUID_DEFAULT = "5e0b0003-0000-4000-8000-000000000003"
ISSUE_URNNBN = "urn:nbn:cz:aba007-p5i001"
VOLUME_URNNBN = "urn:nbn:cz:aba007-p5v001"            # epe/sep: ročník (mutace volume-urnnbn)
SUPPL_UUID = "5e0b0004-0000-4000-8000-000000000004"
ISBN = "978-80-7050-000-1"
ISSN = "2533-784X"
CCNB_VOLUME = "cnb003300001"
CCNB_TITLE = "cnb002883931"

UUID_RE = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
URNNBN_PSP_RE = re.compile(r"^[a-z0-9]{2,6}-[a-z0-9]{6}$")


def part_uuid(kind: int, n: int) -> str:
    """uuid kapitoly/článku n (kind 5 = kapitola, 6 = článek)"""
    return f"5e0b000{kind}-0000-4000-8000-{n:012d}"


METS_NS = ('xmlns:mets="http://www.loc.gov/METS/" xmlns:mods="http://www.loc.gov/mods/v3" '
           'xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" '
           'xmlns:premis="info:lc/xmlns/premis-v2" xmlns:xlink="http://www.w3.org/1999/xlink" '
           'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"')

MUTATIONS = {
    "missing-amdsec": "(sep) bez složky amdsec, souborů amd_mets a fileGrp TECHMDGRP -> SINGLE_SECONDARY-METS-DIR_FILE_FOUND, "
                      "PRIMARY_METS_HEADER_MATCHES_PROFILE (DMF 1.3 kap. 3.1: amdsec povinná v obou variantách)",
    "originaldata-not-in-md5": "(--with-originaldata) soubory originaldata chybí v souboru .md5 (v itemlistu zůstávají) -> "
                               "CHECKSUM_FILE_ALL_PATHS_MATCH_FILES (DMF 3.1 kap. 3.1.3: soubory musí být v info.xml i md5)",
    "originaldata-bad-name": "(--with-originaldata) navíc soubor originaldata/puvodni.pdf bez předpony od_ (v md5 i itemlistu) -> "
                             "NO_EXTRA_FILES_IN_ORIGINALDATA-DIR (kap. 4: předpona od, conv)",
    "supplement-dmdsec": "(emo, epe) navíc příloha: dmdSec MODSMD_SUPPL_0001 a DCMD_SUPPL_0001 -> validní (VEP #20: hlavička "
                         "METS dmdSec přílohy připouští)",
    "volume-no-dc-creator": "(emo, rda) DC svazku bez dc:creator -> validní (VEP #18: v RDA je dc:creator svazku nepovinný)",
    "psp-id-from-title-id": "(epe, sep) název balíčku (PSP-ID) je UUID titulu, ne čísla -> PSP_ID_DERIVED_FROM_IE_ID "
                            "(VEP #21: PSP-ID z identifikátoru intelektuální entity, tj. čísla)",
    "volume-urnnbn": "(epe 2.6, sep) ročník navíc s URN:NBN (MODS i DC) -> validní (DMF 2.6 a 1.3, změnový list: urnnbn "
                     "na úrovni VOLUME povolen)",
    "issue-no-urnnbn": "(epe, sep) číslo bez URN:NBN (MODS i DC) -> MODS/DC_MANDATORY_IDENTIFIERS_PRESENT_LEVEL_ISSUE "
                       "(DMF 2.6: urnnbn čísla M)",
    "wrong-mets-type": "mets/@TYPE 'Monograph' -> DmfDetector rozpozná jako monografii; s vynucenou fDMF "
                       "PRIMARY_METS_HEADER_MATCHES_PROFILE",
    "extra-file-in-original": "cizí soubor original/poznamka.txt (v md5 i itemlistu) -> NO_EXTRA_FILES_IN_ORIGINAL-DIR",
    "pdf-and-epub-both": "(emo, epe) ve složce original PDF i EPUB (dva soubory v OC_EBGRP) -> validní (DMF 3.1 kap. 3.1.2: "
                         "\"archivní kopie souborů např. ve formátu EPUB nebo PDF/A\", zákaz kombinace DMF neuvádí)",
    "info-metadataversion-old": "metadataversion v info.xml o verzi nižší (např. 3.1 -> 3.0): DmfDetector vybere starší fDMF; "
                                "s --with-originaldata -> NO_OTHER_TL_FILES_FOUND (starší fDMF originaldata nezná)",
    "bad-checksum": "špatný MD5 první archivní kopie v souboru .md5 -> CHECKSUM_FILE_ALL_CHECKSUMS_MATCH",
    "missing-original-file": "archivní kopie uvedená v md5, itemlistu a METS chybí na disku -> CHECKSUM_FILE_ALL_PATHS_MATCH_FILES, "
                             "INFO_ITEMLIST_REFERENCES_ALL_FILES",
    "itemtotal-mismatch": "itemtotal v INFO o 1 vyšší -> INFO_ITEMTOTAL_MATCHES_ITEMS_COUNT",
    "oc-filegrp-ebgrp": "(sep) fileGrp archivních kopií OC_EBGRP (jako e-periodika) místo OC_EBSFL -> "
                        "PRIMARY_METS_HEADER_MATCHES_PROFILE jen WARNING (DMF 1.3 kap. 5.6: úvod uvádí OC_EBGRP, "
                        "kap. 5.6.1 OC_EBSFL; nejasnost N2, profil OC_EBGRP úmyslně připouští), balík zůstává validní",
}


def md5_bytes(data: bytes) -> str:
    return hashlib.md5(data).hexdigest()


def esc(s: str) -> str:
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


# ------------------------------------------------------------------------------------------------ MODS/DC šablony


def dmdsec(dmd_id: str, mdtype: str, mods_version: str, body: str) -> str:
    if mdtype == "MODS":
        wrap = f'<mets:mdWrap MDTYPE="MODS" MDTYPEVERSION="{mods_version}" MIMETYPE="text/xml">'
    else:
        wrap = '<mets:mdWrap MDTYPE="DC" MIMETYPE="text/xml">'
    return f"""  <mets:dmdSec ID="{dmd_id}">
    {wrap}
      <mets:xmlData>
{body}      </mets:xmlData>
    </mets:mdWrap>
  </mets:dmdSec>
"""


def src_authority(ctx) -> str:
    # recordContentSource/@authority: DMF s MODS 3.8 "siglaADR" (titul, svazek, článek), dříve "marcorg"
    return "siglaADR" if ctx.mods == "3.8" else "marcorg"


def mods_open(mods_id: str, ctx) -> str:
    # MODS 3.8 (DMF 3.1, 2.6, 1.3): mods/@version povinný; ve verzích s MODS 3.6 jej profily neočekávají
    ver = f' version="{ctx.mods}"' if ctx.mods == "3.8" else ""
    return f'        <mods:mods ID="{mods_id}"{ver}>\n'


def record_info(ctx, with_language=True, with_identifier=False, authority="marcorg", with_standard=True) -> str:
    std = "rda" if ctx.standard == "rda" else "aacr"
    std_xml = f"            <mods:descriptionStandard>{std}</mods:descriptionStandard>\n" if with_standard else ""
    lang = """            <mods:languageOfCataloging>
              <mods:languageTerm authority="iso639-2b">cze</mods:languageTerm>
            </mods:languageOfCataloging>
""" if with_language else ""
    ident = '            <mods:recordIdentifier source="CZ-PrNK">000300001</mods:recordIdentifier>\n' if with_identifier else ""
    return f"""          <mods:recordInfo>
            <mods:recordContentSource authority="{authority}">{SIGLA}</mods:recordContentSource>
            <mods:recordCreationDate encoding="iso8601">20261112T100000</mods:recordCreationDate>
            <mods:recordChangeDate encoding="iso8601">20261112T100000</mods:recordChangeDate>
{ident}{lang}            <mods:recordOrigin>machine generated</mods:recordOrigin>
{std_xml}          </mods:recordInfo>
"""


def physical_location(ctx) -> str:
    # DMF e-monografie a e-periodika 2.3: physicalLocation s type="sigla ADR" (M); od 2.4 authority="siglaADR"
    if ctx.version == "2.3":
        return f'            <mods:physicalLocation type="sigla ADR">{SIGLA}</mods:physicalLocation>\n'
    return f'            <mods:physicalLocation authority="siglaADR">{SIGLA}</mods:physicalLocation>\n'


def forms(ctx, carrier=True) -> str:
    s = '            <mods:form authority="marcform">electronic</mods:form>\n'
    if ctx.standard == "rda" and carrier:
        s += ('            <mods:form authority="rdamedia" type="media">počítač</mods:form>\n'
              '            <mods:form authority="rdacarrier" type="carrier">online zdroj</mods:form>\n')
    return s


def publisher(ctx, name: str) -> str:
    # MODS 3.8 (DMF 3.1, 2.6, 1.3 kap. 5.4): nakladatel v originInfo/agent s roleTerm "publisher", dříve originInfo/publisher
    if ctx.mods == "3.8":
        return f"""            <mods:agent>
              <mods:namePart>{name}</mods:namePart>
              <mods:role>
                <mods:roleTerm>publisher</mods:roleTerm>
              </mods:role>
            </mods:agent>
"""
    return f"            <mods:publisher>{name}</mods:publisher>\n"


def dc_place(ctx) -> str:
    # MODS 3.8: placeTerm se mapuje na dc:publisher (změnový list 2.6, 3.1, 1.3), dříve na dc:coverage
    el = "publisher" if ctx.mods == "3.8" else "coverage"
    return f"          <dc:{el}>xr</dc:{el}>\n          <dc:{el}>Praha</dc:{el}>\n"


def genre(ctx, value: str) -> str:
    # DMF e-periodika 2.3 píše hodnoty genre s podtržítkem (electronic_title), od 2.4 s mezerou (změnový list 2.4)
    return value.replace(" ", "_") if ctx.type == "epe" and ctx.version == "2.3" else value


def event_type(ctx) -> str:
    return ' eventType="publication"' if ctx.standard == "rda" else ""


# ---- e-monografie


def emo_volume(ctx) -> str:
    mods = mods_open("MODS_VOLUME_0001", ctx) + f"""          <mods:titleInfo>
            <mods:title>Syntetická e-monografie</mods:title>
            <mods:subTitle>testovací balíček validátoru</mods:subTitle>
          </mods:titleInfo>
          <mods:name type="personal" usage="primary">
            <mods:namePart type="family">Nováková</mods:namePart>
            <mods:namePart type="given">Jana</mods:namePart>
            <mods:nameIdentifier>jn20000000001</mods:nameIdentifier>
            <mods:role>
              <mods:roleTerm authority="marcrelator" type="code">aut</mods:roleTerm>
            </mods:role>
          </mods:name>
          <mods:typeOfResource>text</mods:typeOfResource>
          <mods:genre>electronic volume</mods:genre>
          <mods:originInfo{event_type(ctx)}>
            <mods:place>
              <mods:placeTerm authority="marccountry" type="code">xr</mods:placeTerm>
            </mods:place>
            <mods:place>
              <mods:placeTerm type="text">Praha</mods:placeTerm>
            </mods:place>
{publisher(ctx, "Národní knihovna České republiky")}            <mods:dateIssued>2026</mods:dateIssued>
            <mods:issuance>single unit</mods:issuance>
          </mods:originInfo>
          <mods:language>
            <mods:languageTerm authority="iso639-2b" type="code">cze</mods:languageTerm>
          </mods:language>
          <mods:physicalDescription>
{forms(ctx)}            <mods:digitalOrigin>born digital</mods:digitalOrigin>
          </mods:physicalDescription>
          <mods:subject authority="czenas">
            <mods:topic>digitalizace</mods:topic>
          </mods:subject>
          <mods:classification authority="udc" edition="Konspekt">025.4</mods:classification>
          <mods:classification authority="Konspekt" edition="Konspekt">20</mods:classification>
          <mods:identifier type="uuid">{VOLUME_UUID}</mods:identifier>
          <mods:identifier type="ccnb">{CCNB_VOLUME}</mods:identifier>
          <mods:identifier type="isbn">{ISBN}</mods:identifier>
          <mods:identifier type="urnnbn">{ctx.volume_urnnbn}</mods:identifier>
          <mods:location>
{physical_location(ctx)}            <mods:shelfLocator>EB 1</mods:shelfLocator>
          </mods:location>
{record_info(ctx, with_identifier=True, authority=src_authority(ctx))}        </mods:mods>
"""
    creator = "" if "volume-no-dc-creator" in ctx.mutations else "          <dc:creator>Nováková, Jana</dc:creator>\n"
    dc = f"""        <oai_dc:dc>
          <dc:title>Syntetická e-monografie: testovací balíček validátoru</dc:title>
{creator}          <dc:type>model:electronicmonograph</dc:type>
{dc_place(ctx)}          <dc:publisher>Národní knihovna České republiky</dc:publisher>
          <dc:date>2026</dc:date>
          <dc:language>cze</dc:language>
          <dc:format>electronic</dc:format>
          <dc:description>born digital</dc:description>
          <dc:subject>digitalizace</dc:subject>
          <dc:subject>025.4</dc:subject>
          <dc:subject>20</dc:subject>
          <dc:identifier>uuid:{VOLUME_UUID}</dc:identifier>
          <dc:identifier>ccnb:{CCNB_VOLUME}</dc:identifier>
          <dc:identifier>isbn:{ISBN}</dc:identifier>
          <dc:identifier>urnnbn:{ctx.volume_urnnbn}</dc:identifier>
          <dc:source>{SIGLA}</dc:source>
          <dc:source>EB 1</dc:source>
        </oai_dc:dc>
"""
    return (dmdsec("MODSMD_VOLUME_0001", "MODS", ctx.mods, mods) +
            dmdsec("DCMD_VOLUME_0001", "DC", ctx.mods, dc))


def emo_chapter(ctx, n: int) -> str:
    uid = part_uuid(5, n)
    mods = mods_open(f"MODS_CHAP_{n:04d}", ctx) + f"""          <mods:titleInfo>
            <mods:title>Kapitola {n}</mods:title>
          </mods:titleInfo>
          <mods:genre type="chapter">chapter</mods:genre>
          <mods:language>
            <mods:languageTerm authority="iso639-2b" type="code">cze</mods:languageTerm>
          </mods:language>
          <mods:physicalDescription>
            <mods:form authority="marcform">electronic</mods:form>
            <mods:digitalOrigin>born digital</mods:digitalOrigin>
          </mods:physicalDescription>
          <mods:identifier type="uuid">{uid}</mods:identifier>
          <mods:recordInfo>
            <mods:recordContentSource>{SIGLA}</mods:recordContentSource>
            <mods:recordCreationDate encoding="iso8601">20261112T100000</mods:recordCreationDate>
            <mods:recordOrigin>machine generated</mods:recordOrigin>
          </mods:recordInfo>
        </mods:mods>
"""
    dc = f"""        <oai_dc:dc>
          <dc:title>Kapitola {n}</dc:title>
          <dc:type>model:internalpart</dc:type>
          <dc:language>cze</dc:language>
          <dc:format>electronic</dc:format>
          <dc:description>born digital</dc:description>
          <dc:identifier>uuid:{uid}</dc:identifier>
        </oai_dc:dc>
"""
    return (dmdsec(f"MODSMD_CHAP_{n:04d}", "MODS", ctx.mods, mods) +
            dmdsec(f"DCMD_CHAP_{n:04d}", "DC", ctx.mods, dc))


def supplement(ctx, parent_title: str) -> str:
    # DMF 3.1 kap. 5.4.5 (a 2.6): form/@type je M s hodnotami media, carrier, form authority="marcform" bez type tedy
    # neprojde; s MODS 3.8 jen formy rdamedia a rdacarrier
    suppl_forms = forms(ctx)
    # e-periodika: genre přílohy s type volume_supplement/issue_supplement (M) a originInfo s eventType (MA)
    if ctx.type == "epe":
        # e-periodika 2.6: profil přílohy eventType nezná
        ev = "" if ctx.mods == "3.8" else ' eventType="publication"'
        suppl_genre = f"""          <mods:genre type="issue_supplement">supplement</mods:genre>
          <mods:originInfo{ev}>
            <mods:dateIssued>12.11.2026</mods:dateIssued>
          </mods:originInfo>
"""
    else:
        suppl_genre = "          <mods:genre>supplement</mods:genre>\n"
    if ctx.mods == "3.8":
        suppl_forms = "".join(l + "\n" for l in suppl_forms.splitlines() if 'type="' in l)
    mods = mods_open("MODS_SUPPL_0001", ctx) + f"""          <mods:titleInfo>
            <mods:title>{parent_title}</mods:title>
            <mods:partNumber>Příloha 1</mods:partNumber>
            <mods:partName>Tabulky</mods:partName>
          </mods:titleInfo>
          <mods:typeOfResource>text</mods:typeOfResource>
{suppl_genre}          <mods:language>
            <mods:languageTerm authority="iso639-2b" type="code">cze</mods:languageTerm>
          </mods:language>
          <mods:physicalDescription>
{suppl_forms}            <mods:extent>4 strany</mods:extent>
          </mods:physicalDescription>
          <mods:identifier type="uuid">{SUPPL_UUID}</mods:identifier>
{record_info(ctx, with_language=False, authority=src_authority(ctx), with_standard=ctx.mods != "3.8")}        </mods:mods>
"""
    dc = f"""        <oai_dc:dc>
          <dc:title>{parent_title}</dc:title>
          <dc:title>Tabulky</dc:title>
          <dc:description>Příloha 1</dc:description>
          <dc:type>model:supplement</dc:type>
          <dc:language>cze</dc:language>
          <dc:identifier>uuid:{SUPPL_UUID}</dc:identifier>
          <dc:format>4 strany</dc:format>
        </oai_dc:dc>
"""
    return (dmdsec("MODSMD_SUPPL_0001", "MODS", ctx.mods, mods) +
            dmdsec("DCMD_SUPPL_0001", "DC", ctx.mods, dc))


# ---- e-periodika a skládaná e-periodika

PERIO_TITLE = "A / Věda a výzkum"


def perio_title(ctx) -> str:
    # DMF e-periodika 2.3 a 2.4: classification Konspekt s edition="Konspekt" (M), dc:type model:electronic_periodical
    old = ctx.type == "epe" and ctx.version in ("2.3", "2.4")
    konspekt_edition = ' edition="Konspekt"' if old else ""
    dc_model = "model:electronic_periodical" if old else "model:electronicperiodical"
    mods = mods_open("MODS_TITLE_0001", ctx) + f"""          <mods:titleInfo>
            <mods:title>{PERIO_TITLE}</mods:title>
          </mods:titleInfo>
          <mods:name type="corporate">
            <mods:namePart>Akademie věd České republiky</mods:namePart>
            <mods:nameIdentifier>kn20010709016</mods:nameIdentifier>
          </mods:name>
          <mods:typeOfResource>text</mods:typeOfResource>
          <mods:genre>{genre(ctx, "electronic title")}</mods:genre>
          <mods:originInfo{event_type(ctx)}>
            <mods:place>
              <mods:placeTerm authority="marccountry" type="code">xr</mods:placeTerm>
            </mods:place>
            <mods:place>
              <mods:placeTerm type="text">Praha</mods:placeTerm>
            </mods:place>
{publisher(ctx, "Středisko společných činností AV ČR")}            <mods:dateIssued>2017-</mods:dateIssued>
            <mods:issuance>continuing</mods:issuance>
            <mods:frequency>4x ročně</mods:frequency>
          </mods:originInfo>
          <mods:language>
            <mods:languageTerm authority="iso639-2b" type="code">cze</mods:languageTerm>
          </mods:language>
          <mods:physicalDescription>
{forms(ctx)}            <mods:digitalOrigin>born digital</mods:digitalOrigin>
          </mods:physicalDescription>
          <mods:identifier type="uuid">{TITLE_UUID}</mods:identifier>
          <mods:identifier type="issn">{ISSN}</mods:identifier>
          <mods:identifier type="ccnb">{CCNB_TITLE}</mods:identifier>
          <mods:location>
{physical_location(ctx)}            <mods:shelfLocator>PD 642</mods:shelfLocator>
          </mods:location>
          <mods:subject authority="czenas">
            <mods:topic>věda</mods:topic>
          </mods:subject>
          <mods:classification authority="udc" edition="Konspekt">001</mods:classification>
          <mods:classification authority="Konspekt"{konspekt_edition}>12</mods:classification>
{record_info(ctx, with_language=False, with_identifier=True, authority=src_authority(ctx))}        </mods:mods>
"""
    dc = f"""        <oai_dc:dc>
          <dc:title>{PERIO_TITLE}</dc:title>
          <dc:creator>Akademie věd České republiky</dc:creator>
          <dc:type>{dc_model}</dc:type>
{dc_place(ctx)}          <dc:publisher>Středisko společných činností AV ČR</dc:publisher>
          <dc:date>2017-</dc:date>
          <dc:description>born digital</dc:description>
          <dc:identifier>uuid:{TITLE_UUID}</dc:identifier>
          <dc:identifier>issn:{ISSN}</dc:identifier>
          <dc:identifier>ccnb:{CCNB_TITLE}</dc:identifier>
          <dc:source>{SIGLA}</dc:source>
          <dc:source>PD 642</dc:source>
          <dc:subject>věda</dc:subject>
          <dc:subject>001</dc:subject>
          <dc:subject>12</dc:subject>
        </oai_dc:dc>
"""
    return (dmdsec("MODSMD_TITLE_0001", "MODS", ctx.mods, mods) +
            dmdsec("DCMD_TITLE_0001", "DC", ctx.mods, dc))


def perio_volume(ctx) -> str:
    urn_mods = urn_dc = ""
    if "volume-urnnbn" in ctx.mutations:
        urn_mods = f'          <mods:identifier type="urnnbn">{VOLUME_URNNBN}</mods:identifier>\n'
        urn_dc = f"          <dc:identifier>urnnbn:{VOLUME_URNNBN}</dc:identifier>\n"
    # recordInfo ročníku: profil skládaných 1.3 jej nezná (DMF 1.3 kap. 5.4.2)
    volume_record_info = "" if ctx.type == "sep" else """          <mods:recordInfo>
            <mods:recordCreationDate encoding="iso8601">20261112T100000</mods:recordCreationDate>
            <mods:recordChangeDate encoding="iso8601">20261112T100000</mods:recordChangeDate>
            <mods:recordOrigin>machine generated</mods:recordOrigin>
          </mods:recordInfo>
"""
    mods = mods_open("MODS_VOLUME_0001", ctx) + f"""          <mods:titleInfo>
            <mods:partNumber>9</mods:partNumber>
          </mods:titleInfo>
          <mods:genre>{genre(ctx, "electronic volume")}</mods:genre>
          <mods:originInfo>
            <mods:dateIssued>2026</mods:dateIssued>
          </mods:originInfo>
          <mods:identifier type="uuid">{VOLUME_UUID}</mods:identifier>
{urn_mods}{volume_record_info}        </mods:mods>
"""
    # dc:title ročníku: profily 2.6 a 1.3 jej vyžadují (DMF 2.6 kap. 5.4.2 mapuje titleInfo/title na dc:title)
    title = f"          <dc:title>{PERIO_TITLE}</dc:title>\n" if ctx.mods == "3.8" else ""
    dc = f"""        <oai_dc:dc>
{title}{"" if ctx.mods == "3.8" else "          <dc:description>9</dc:description>" + chr(10)}          <dc:type>model:periodicalvolume</dc:type>
          <dc:identifier>uuid:{VOLUME_UUID}</dc:identifier>
{urn_dc}          <dc:date>2026</dc:date>
        </oai_dc:dc>
"""
    return (dmdsec("MODSMD_VOLUME_0001", "MODS", ctx.mods, mods) +
            dmdsec("DCMD_VOLUME_0001", "DC", ctx.mods, dc))


def perio_issue(ctx) -> str:
    urn_mods = urn_dc = ""
    if "issue-no-urnnbn" not in ctx.mutations:
        urn_mods = f'          <mods:identifier type="urnnbn">{ISSUE_URNNBN}</mods:identifier>\n'
        urn_dc = f"          <dc:identifier>urnnbn:{ISSUE_URNNBN}</dc:identifier>\n"
    mods = mods_open("MODS_ISSUE_0001", ctx) + f"""          <mods:titleInfo>
            <mods:title>{PERIO_TITLE}</mods:title>
            <mods:partNumber>1</mods:partNumber>
          </mods:titleInfo>
          <mods:genre type="normal">{genre(ctx, "electronic issue")}</mods:genre>
          <mods:originInfo>
            <mods:place>
              <mods:placeTerm authority="marccountry" type="code">xr</mods:placeTerm>
            </mods:place>
            <mods:place>
              <mods:placeTerm type="text">Praha</mods:placeTerm>
            </mods:place>
{publisher(ctx, "Středisko společných činností AV ČR")}            <mods:dateIssued>12.11.2026</mods:dateIssued>
          </mods:originInfo>
          <mods:language>
            <mods:languageTerm authority="iso639-2b" type="code">cze</mods:languageTerm>
          </mods:language>
          <mods:physicalDescription>
            <mods:digitalOrigin>born digital</mods:digitalOrigin>
          </mods:physicalDescription>
          <mods:location>
{physical_location(ctx)}            <mods:shelfLocator>PD 642</mods:shelfLocator>
          </mods:location>
{urn_mods}          <mods:identifier type="uuid">{ctx.issue_uuid}</mods:identifier>
{record_info(ctx, with_language=False, with_standard=ctx.mods != "3.8")}        </mods:mods>
"""
    dc = f"""        <oai_dc:dc>
          <dc:title>{PERIO_TITLE}</dc:title>
          <dc:description>1</dc:description>
          <dc:type>model:periodicalitem</dc:type>
          <dc:date>12.11.2026</dc:date>
{dc_place(ctx)}          <dc:publisher>Středisko společných činností AV ČR</dc:publisher>
          <dc:language>cze</dc:language>
{urn_dc}          <dc:identifier>uuid:{ctx.issue_uuid}</dc:identifier>
          <dc:description>born digital</dc:description>
          <dc:source>{SIGLA}</dc:source>
          <dc:source>PD 642</dc:source>
        </oai_dc:dc>
"""
    return (dmdsec("MODSMD_ISSUE_0001", "MODS", ctx.mods, mods) +
            dmdsec("DCMD_ISSUE_0001", "DC", ctx.mods, dc))


# genre/@type článku: hodnoty podle profilů (DMF 2.3 a 2.4 malými písmeny s mezerou, 2.5+ camelCase, 1.3 vlastní výčet)
ARTICLE_TYPES = {
    ("epe", "2.3"): ["editorsNote", "main article"],
    ("epe", "2.4"): ["editorsNote", "main article"],
    ("epe", "2.5"): ["editorial", "mainArticle"],
    ("epe", "2.6"): ["editorial", "mainArticle"],
    ("sep", "1.3"): ["editorsNote", "main article"],
}


def article_type(ctx, n: int) -> str:
    types = ARTICLE_TYPES[(ctx.type, ctx.version)]
    return types[0] if n == 1 else types[1]


def perio_article(ctx, n: int) -> str:
    uid = part_uuid(6, n)
    gtype = article_type(ctx, n)
    mods = mods_open(f"MODS_ART_{n:04d}", ctx) + f"""          <mods:titleInfo>
            <mods:title>Článek {n}</mods:title>
          </mods:titleInfo>
          <mods:name type="personal">
            <mods:namePart type="family">Svoboda</mods:namePart>
            <mods:namePart type="given">Petr</mods:namePart>
            <mods:role>
              <mods:roleTerm authority="marcrelator" type="code">aut</mods:roleTerm>
            </mods:role>
          </mods:name>
          <mods:genre type="{gtype}">{genre(ctx, "electronic article")}</mods:genre>
          <mods:language>
            <mods:languageTerm authority="iso639-2b" type="code">cze</mods:languageTerm>
          </mods:language>
          <mods:physicalDescription>
            <mods:form>electronic</mods:form>
            <mods:digitalOrigin>born digital</mods:digitalOrigin>
          </mods:physicalDescription>
          <mods:identifier type="uuid">{uid}</mods:identifier>
{record_info(ctx, with_language=False, authority="marcorg" if ctx.type == "epe" else src_authority(ctx))}        </mods:mods>
"""
    dc = f"""        <oai_dc:dc>
          <dc:title>Článek {n}</dc:title>
          <dc:creator>Svoboda, Petr</dc:creator>
          <dc:type>model:internalpart</dc:type>
          <dc:language>cze</dc:language>
          <dc:format>electronic</dc:format>
          <dc:description>born digital</dc:description>
          <dc:identifier>uuid:{uid}</dc:identifier>
        </oai_dc:dc>
"""
    return (dmdsec(f"MODSMD_ART_{n:04d}", "MODS", ctx.mods, mods) +
            dmdsec(f"DCMD_ART_{n:04d}", "DC", ctx.mods, dc))


# ------------------------------------------------------------------------------------------------ PREMIS, hlavička

def mets_hdr(indent="  ") -> str:
    return f"""{indent}<mets:metsHdr CREATEDATE="{CREATED}" LASTMODDATE="{CREATED}">
{indent}  <mets:agent ROLE="CREATOR" TYPE="ORGANIZATION">
{indent}    <mets:name>{SIGLA}</mets:name>
{indent}  </mets:agent>
{indent}  <mets:agent ROLE="ARCHIVIST" TYPE="ORGANIZATION">
{indent}    <mets:name>{SIGLA}</mets:name>
{indent}  </mets:agent>
{indent}</mets:metsHdr>
"""


FORMAT_INFO = {
    # přípona: (MIME, formatName, formatVersion, PRONOM, aplikace)
    "pdf": ("application/pdf", "application/pdf", "1b", "fmt/95", "GPL Ghostscript"),
    "epub": ("application/epub+zip", "application/epub+zip", "3.0", "fmt/483", "calibre"),
    "od": ("application/pdf", "application/pdf", "1.4", "fmt/18", "Microsoft Word"),
}


def premis_object(tech_id, ident, plv, data: bytes, fmt_key, orig_name, rel=None, events=(), ext=True) -> str:
    _, fname, fver, puid, app = FORMAT_INFO[fmt_key]
    ext_xml = ""
    if ext:
        ext_xml = """
              <premis:objectCharacteristicsExtension>
                <docmd:document xmlns:docmd="http://www.fcla.edu/docmd">
                  <docmd:PageCount>1</docmd:PageCount>
                  <docmd:Language>cze</docmd:Language>
                  <docmd:documentMetadataExtension>
                    <ndktech:ndktech xmlns:ndktech="http://www.ndk.cz/standardy-digitalizace/ndktech/1.1">
                      <ndktech:imagesCount>0</ndktech:imagesCount>
                      <ndktech:indirectObjectsNumber>4</ndktech:indirectObjectsNumber>
                    </ndktech:ndktech>
                  </docmd:documentMetadataExtension>
                </docmd:document>
              </premis:objectCharacteristicsExtension>"""
    rel_xml = ""
    if rel:
        rel_xml = f"""
            <premis:relationship>
              <premis:relationshipType>derivation</premis:relationshipType>
              <premis:relationshipSubType>created from</premis:relationshipSubType>
              <premis:relatedObjectIdentification>
                <premis:relatedObjectIdentifierType>ObjectID</premis:relatedObjectIdentifierType>
                <premis:relatedObjectIdentifierValue>{rel}</premis:relatedObjectIdentifierValue>
              </premis:relatedObjectIdentification>
            </premis:relationship>"""
    ev_xml = "".join(f"""
            <premis:linkingEventIdentifier>
              <premis:linkingEventIdentifierType>EventID</premis:linkingEventIdentifierType>
              <premis:linkingEventIdentifierValue>{e}</premis:linkingEventIdentifierValue>
            </premis:linkingEventIdentifier>""" for e in events)
    return f"""    <mets:techMD ID="{tech_id}">
      <mets:mdWrap MDTYPE="PREMIS" MIMETYPE="text/xml">
        <mets:xmlData>
          <premis:object xsi:type="premis:file">
            <premis:objectIdentifier>
              <premis:objectIdentifierType>ObjectID</premis:objectIdentifierType>
              <premis:objectIdentifierValue>{ident}</premis:objectIdentifierValue>
            </premis:objectIdentifier>
            <premis:preservationLevel>
              <premis:preservationLevelValue>{plv}</premis:preservationLevelValue>
              <premis:preservationLevelDateAssigned>{CREATED_DAY}</premis:preservationLevelDateAssigned>
            </premis:preservationLevel>
            <premis:objectCharacteristics>
              <premis:compositionLevel>0</premis:compositionLevel>
              <premis:fixity>
                <premis:messageDigestAlgorithm>MD5</premis:messageDigestAlgorithm>
                <premis:messageDigest>{md5_bytes(data)}</premis:messageDigest>
                <premis:messageDigestOriginator>JHOVE</premis:messageDigestOriginator>
              </premis:fixity>
              <premis:size>{len(data)}</premis:size>
              <premis:format>
                <premis:formatDesignation>
                  <premis:formatName>{fname}</premis:formatName>
                  <premis:formatVersion>{fver}</premis:formatVersion>
                </premis:formatDesignation>
                <premis:formatRegistry>
                  <premis:formatRegistryName>PRONOM</premis:formatRegistryName>
                  <premis:formatRegistryKey>{puid}</premis:formatRegistryKey>
                </premis:formatRegistry>
              </premis:format>
              <premis:creatingApplication>
                <premis:creatingApplicationName>{app}</premis:creatingApplicationName>
                <premis:creatingApplicationVersion>1.0</premis:creatingApplicationVersion>
                <premis:dateCreatedByApplication>{CREATED}</premis:dateCreatedByApplication>
              </premis:creatingApplication>{ext_xml}
            </premis:objectCharacteristics>
            <premis:originalName>{orig_name}</premis:originalName>{rel_xml}{ev_xml}
          </premis:object>
        </mets:xmlData>
      </mets:mdWrap>
    </mets:techMD>
"""


def premis_event(md_id, ident, etype, agent, objs) -> str:
    lo = "".join(f"""
            <premis:linkingObjectIdentifier>
              <premis:linkingObjectIdentifierType>ObjectID</premis:linkingObjectIdentifierType>
              <premis:linkingObjectIdentifierValue>{o}</premis:linkingObjectIdentifierValue>
            </premis:linkingObjectIdentifier>""" for o in objs)
    return f"""    <mets:digiprovMD ID="{md_id}">
      <mets:mdWrap MDTYPE="PREMIS" MIMETYPE="text/xml">
        <mets:xmlData>
          <premis:event>
            <premis:eventIdentifier>
              <premis:eventIdentifierType>EventID</premis:eventIdentifierType>
              <premis:eventIdentifierValue>{ident}</premis:eventIdentifierValue>
            </premis:eventIdentifier>
            <premis:eventType>{etype}</premis:eventType>
            <premis:eventDateTime>{CREATED}</premis:eventDateTime>
            <premis:eventDetail>{etype}</premis:eventDetail>
            <premis:eventOutcomeInformation>
              <premis:eventOutcome>successful</premis:eventOutcome>
            </premis:eventOutcomeInformation>
            <premis:linkingAgentIdentifier>
              <premis:linkingAgentIdentifierType>AgentID</premis:linkingAgentIdentifierType>
              <premis:linkingAgentIdentifierValue>{agent}</premis:linkingAgentIdentifierValue>
              <premis:linkingAgentRole>software</premis:linkingAgentRole>
            </premis:linkingAgentIdentifier>{lo}
          </premis:event>
        </mets:xmlData>
      </mets:mdWrap>
    </mets:digiprovMD>
"""


def premis_agent(md_id, ident, name) -> str:
    return f"""    <mets:digiprovMD ID="{md_id}">
      <mets:mdWrap MDTYPE="PREMIS" MIMETYPE="text/xml">
        <mets:xmlData>
          <premis:agent>
            <premis:agentIdentifier>
              <premis:agentIdentifierType>AgentID</premis:agentIdentifierType>
              <premis:agentIdentifierValue>{ident}</premis:agentIdentifierValue>
            </premis:agentIdentifier>
            <premis:agentName>{name}</premis:agentName>
            <premis:agentType>software</premis:agentType>
          </premis:agent>
        </mets:xmlData>
      </mets:mdWrap>
    </mets:digiprovMD>
"""


def rights(ctx) -> str:
    # MIMETYPE mdWrap autorskoprávních metadat: DMF e-monografie a e-periodika 2.3, 2.4 předepisují "text/html",
    # od 3.0 / 2.5 (změnový list, kap. 3.9) "text/xml"
    mime = "text/html" if ctx.type in ("emo", "epe") and ctx.version in ("2.3", "2.4") else "text/xml"
    return f"""  <mets:amdSec ID="AMD_RIGHTS_0001">
    <mets:rightsMD ID="RIGHTS_0001">
      <mets:mdWrap MDTYPE="OTHER" MIMETYPE="{mime}" OTHERMDTYPE="CopyrightMD">
        <mets:xmlData>
          <copyright copyright.status="copyrighted" publication.status="published"/>
        </mets:xmlData>
      </mets:mdWrap>
    </mets:rightsMD>
  </mets:amdSec>
"""


def file_entry(fid, rel, data: bytes, mime, seq, admid=None, indent="      ") -> str:
    adm = f' ADMID="{admid}"' if admid else ""
    return (f'{indent}<mets:file ID="{fid}" MIMETYPE="{mime}" SIZE="{len(data)}" CHECKSUMTYPE="MD5" '
            f'CHECKSUM="{md5_bytes(data)}" SEQ="{seq}" CREATED="{CREATED}"{adm}>\n'
            f'{indent}  <mets:FLocat LOCTYPE="URL" xlink:href="{rel}"/>\n'
            f'{indent}</mets:file>\n')


# ------------------------------------------------------------------------------------------------ generátor


class Ctx:
    pass


def generate(target, ptype, version, fmt="pdf", standard="rda", chapters=1, articles=2, with_supplement=False,
             with_originaldata=False, mutations=(), pdf=DEFAULT_PDF, epub=DEFAULT_EPUB, od=DEFAULT_OD):
    mutations = set(mutations)
    unknown = mutations - set(MUTATIONS)
    if unknown:
        sys.exit("neznámé mutace: " + ", ".join(sorted(unknown)) + "; známé: " + ", ".join(sorted(MUTATIONS)))
    if (ptype, version) not in FDMF:
        sys.exit(f"nepodporovaná kombinace --type {ptype} --version {version}; podporované: "
                 + ", ".join(f"{t} {v}" for t, v in sorted(FDMF)))
    mods_version, od_allowed = FDMF[(ptype, version)]
    if with_originaldata and not od_allowed:
        sys.exit(f"složka originaldata je až v DMF e-monografie 3.1, e-periodika 2.6 a skládaná 1.3, ne v {ptype} {version}")

    # mutace, které by v dané kombinaci nic nezměnily (nebo nedávají smysl)
    def need(cond, names, msg):
        bad = set(names) & mutations
        if bad and not cond:
            sys.exit("mutace " + ", ".join(sorted(bad)) + " " + msg)
    need(ptype == "sep", {"missing-amdsec", "oc-filegrp-ebgrp"}, "mají smysl jen pro --type sep")
    need(with_originaldata, {"originaldata-not-in-md5", "originaldata-bad-name"}, "potřebují --with-originaldata")
    need(ptype in ("emo", "epe"), {"supplement-dmdsec", "pdf-and-epub-both"}, "mají smysl jen pro --type emo a epe")
    need(not with_supplement, {"supplement-dmdsec"}, "nic nezmění, příloha už je dána --with-supplement")
    need(ptype == "emo" and standard == "rda", {"volume-no-dc-creator"}, "má smysl jen pro --type emo --standard rda")
    need(ptype in ("epe", "sep"), {"psp-id-from-title-id", "issue-no-urnnbn"}, "mají smysl jen pro --type epe a sep")
    need(ptype == "sep" or (ptype == "epe" and version == "2.6"), {"volume-urnnbn"},
         "má smysl jen pro e-periodika 2.6 a skládaná 1.3 (starší DMF urnnbn ročníku nepovolují)")
    need((ptype, version) in PREVIOUS, {"info-metadataversion-old"}, "nemá pro tuto verzi starší podporovanou fDMF")

    psp = os.path.basename(os.path.normpath(target))
    ctx = Ctx()
    ctx.type, ctx.version, ctx.mods, ctx.standard, ctx.mutations = ptype, version, mods_version, standard, mutations
    ctx.target = target
    if ptype == "emo":
        if not URNNBN_PSP_RE.match(psp):
            sys.exit(f"pro e-monografii musí být název balíčku druhá část URN:NBN (např. aba007-p5m301), ne '{psp}'")
        ctx.volume_urnnbn = f"urn:nbn:cz:{psp}"
    else:
        if not UUID_RE.match(psp):
            sys.exit(f"pro e-periodikum musí být název balíčku UUID čísla, ne '{psp}'")
        # psp-id-from-title-id: adresář pojmenovaný podle UUID titulu, číslo má jiné UUID
        ctx.issue_uuid = ISSUE_UUID_DEFAULT if "psp-id-from-title-id" in mutations else psp
    if "psp-id-from-title-id" in mutations:
        global TITLE_UUID
        TITLE_UUID = psp

    if os.path.exists(target):
        shutil.rmtree(target)
    os.makedirs(target)
    written = {}  # rel (bez úvodního /) -> data

    def put(rel, data):
        if isinstance(data, str):
            data = data.encode("utf-8")
        path = os.path.join(target, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(data)
        written[rel] = data
        return data

    def read(p):
        with open(p, "rb") as f:
            return f.read()

    # ---- archivní kopie
    oc_files = []  # (rel, data, ext)
    if ptype == "sep":
        for n in range(1, articles + 1):
            ext = fmt
            rel = f"original/oc_{psp}_{n:04d}.{ext}"
            oc_files.append((rel, put(rel, read(pdf if ext == "pdf" else epub)), ext))
    else:
        exts = ["pdf", "epub"] if "pdf-and-epub-both" in mutations else [fmt]
        for i, ext in enumerate(exts, 1):
            suffix = f"_{i:04d}" if len(exts) > 1 else ""
            rel = f"original/oc_{psp}{suffix}.{ext}"
            oc_files.append((rel, put(rel, read(pdf if ext == "pdf" else epub)), ext))
    if "extra-file-in-original" in mutations:
        put("original/poznamka.txt", "poznámka, která do složky original nepatří\n")

    # ---- originaldata (DMF 3.1/2.6 kap. 3.1.3, 1.3 kap. 3.1)
    od_files = []
    if with_originaldata:
        num = "_0001" if ptype == "sep" else ""
        od_rel = f"originaldata/od_{psp}{num}.pdf"
        conv_rel = f"originaldata/conv_{psp}{num}.xml"
        od_files.append(od_rel)
        put(od_rel, read(od))
        put(conv_rel, f"""<?xml version="1.0" encoding="UTF-8"?>
<conversion>
  <source>od_{psp}{num}.pdf</source>
  <target>{os.path.basename(oc_files[0][0])}</target>
  <tool>GPL Ghostscript 10.0</tool>
  <result>PDF/A-1b</result>
</conversion>
""")
        od_files.append(conv_rel)
        if "originaldata-bad-name" in mutations:
            put("originaldata/puvodni.pdf", read(od))
            od_files.append("originaldata/puvodni.pdf")

    # ---- hlavní METS
    if ptype == "emo":
        mets = build_emo_mets(ctx, psp, oc_files, od_files, chapters, with_supplement or "supplement-dmdsec" in mutations)
    elif ptype == "epe":
        mets = build_epe_mets(ctx, psp, oc_files, od_files, articles, with_supplement or "supplement-dmdsec" in mutations)
    else:
        mets = build_sep_mets(ctx, psp, oc_files, od_files, put)
    if "wrong-mets-type" in mutations:
        old = f'TYPE="{METS_TYPE[ptype]}"'
        assert mets.count(old) == 1
        mets = mets.replace(old, 'TYPE="Monograph"')
    put(f"mets_{psp}.xml", mets)

    if "missing-original-file" in mutations:
        os.remove(os.path.join(target, oc_files[0][0]))

    # ---- MD5: všechny soubory kromě info a md5 (kap. 3.1.5), cesty absolutní vůči kořeni balíčku
    md5_name = f"md5_{psp}.md5"
    md5_rels = sorted(written)
    if "originaldata-not-in-md5" in mutations:
        md5_rels = [r for r in md5_rels if not r.startswith("originaldata/")]
    md5_lines = "".join(f"{md5_bytes(written[r])} /{r}\n" for r in md5_rels)
    if "bad-checksum" in mutations:
        first = oc_files[0][0]
        line = f"{md5_bytes(written[first])} /{first}\n"
        assert md5_lines.count(line) == 1
        md5_lines = md5_lines.replace(line, f"{'0' * 32} /{first}\n")
    put(md5_name, md5_lines)

    # ---- INFO (kap. 5.1): itemlist všech souborů vč. info.xml, size v kB
    info_name = f"info_{psp}.xml"
    items = sorted(written) + [info_name]
    size_kb = max(1, sum(len(d) for d in written.values()) // 1024)
    md_version = PREVIOUS[(ptype, version)] if "info-metadataversion-old" in mutations else version
    put(info_name, info_xml(ctx, psp, md_version, items, size_kb, md5_name, md5_bytes(written[md5_name]), mutations))
    return target


def info_xml(ctx, psp, md_version, items, size_kb, md5_name, md5sum, mutations):
    if ctx.type == "emo":
        ids = [("uuid", VOLUME_UUID), ("urnnbn", ctx.volume_urnnbn), ("ccnb", CCNB_VOLUME), ("isbn", ISBN)]
    else:
        # titleid = identifikátory titulu; info XSD e-periodik a skládaných urnnbn nepřipouští
        ids = [("uuid", TITLE_UUID), ("issn", ISSN), ("ccnb", CCNB_TITLE)]
    itemtotal = len(items) + (1 if "itemtotal-mismatch" in mutations else 0)
    # validation: DMF 3.1, 2.6 kap. 5.1 MA s povinným version, 1.3 M
    return (f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<info>
  <created>{CREATED}</created>
  <metadataversion>{md_version}</metadataversion>
  <packageid>{psp}</packageid>
  <mainmets>mets_{psp}.xml</mainmets>
  <validation version="3.0">Valid</validation>
""" + "".join(f'  <titleid type="{t}">{v}</titleid>\n' for t, v in ids) + f"""  <institution>{SIGLA}</institution>
  <creator>{SIGLA}</creator>
  <size>{size_kb}</size>
  <itemlist itemtotal="{itemtotal}">
""" + "".join(f"    <item>/{i}</item>\n" for i in items) + f"""  </itemlist>
  <checksum type="md5" checksum="{md5sum}">{md5_name}</checksum>
</info>
""")


def primary_amdsec(amd_id, oc_files, od_files, od_data):
    """PREMIS objekty archivních kopií (logical preservation) a původního souboru (bit-level), události, agenti"""
    objs, events, agents = "", "", ""
    oc_ids = []
    od_id = None
    if od_files:
        od_name = os.path.basename(od_files[0])
        od_id = "od_0001"
    for i, (rel, data, ext) in enumerate(oc_files, 1):
        ident = f"oc_{i:04d}"
        oc_ids.append(ident)
        rel_to = od_id if (od_id and i == 1) else None
        objs += premis_object(f"OBJ_{i:03d}", ident, "logical preservation", data, ext, os.path.basename(rel),
                              rel=rel_to, events=["event_001"])
    n = len(oc_files)
    if od_id:
        objs += premis_object(f"OBJ_{n + 1:03d}", od_id, "bit-level", od_data, "od", od_name,
                              events=["event_001"], ext=False)
        events += premis_event("EVT_001", "event_001", "migration", "agent_001", [od_id] + oc_ids)
        agents += premis_agent("AGENT_001", "agent_001", "GPL Ghostscript 10.0")
    else:
        events += premis_event("EVT_001", "event_001", "creation", "agent_001", oc_ids)
        agents += premis_agent("AGENT_001", "agent_001", "calibre 7.0")
    return f'  <mets:amdSec ID="{amd_id}">\n{objs}{events}{agents}  </mets:amdSec>\n'


def oc_filesec(psp, oc_files, grp="OC_EBGRP"):
    fs = f'  <mets:fileSec>\n    <mets:fileGrp ID="{grp}" USE="master">\n'
    for i, (rel, data, ext) in enumerate(oc_files, 1):
        fid = os.path.splitext(os.path.basename(rel))[0]
        fs += file_entry(fid, "/" + rel, data, FORMAT_INFO[ext][0], i)
    return fs + "    </mets:fileGrp>\n  </mets:fileSec>\n"


def doc_divs(oc_files, indent):
    s = ""
    for i, (rel, data, ext) in enumerate(oc_files, 1):
        fid = os.path.splitext(os.path.basename(rel))[0]
        s += (f'{indent}<mets:div ID="DOCUMENT_{i:04d}" TYPE="DOCUMENT" LABEL="{fid}">\n'
              f'{indent}  <mets:div ID="FILE_{i:04d}" TYPE="FILE" LABEL="{fid}" ADMID="OBJ_{i:03d}">\n'
              f'{indent}    <mets:fptr FILEID="{fid}"/>\n'
              f'{indent}  </mets:div>\n'
              f'{indent}</mets:div>\n')
    return s


def build_emo_mets(ctx, psp, oc_files, od_files, chapters, with_suppl):
    dmd = emo_volume(ctx) + "".join(emo_chapter(ctx, n) for n in range(1, chapters + 1))
    if with_suppl:
        dmd += supplement(ctx, "Syntetická e-monografie")
    chap_divs = "".join(f'        <mets:div ID="CHAP_{n:04d}" TYPE="CHAPTER" DMDID="MODSMD_CHAP_{n:04d}" '
                        f'LABEL="Kapitola {n}" ORDER="{n}"/>\n' for n in range(1, chapters + 1))
    suppl_div = ('        <mets:div ID="SUPPL_0001" TYPE="SUPPLEMENT" DMDID="MODSMD_SUPPL_0001" LABEL="Tabulky"/>\n'
                 if with_suppl else "")
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<mets:mets {METS_NS} LABEL="Syntetická e-monografie: testovací balíček validátoru, 2026" TYPE="{METS_TYPE['emo']}">
{mets_hdr()}{dmd}{primary_amdsec("AMD_VOLUME_0001", oc_files, od_files, open_od(ctx, od_files))}{rights(ctx)}{oc_filesec(psp, oc_files)}  <mets:structMap>
    <mets:div ID="VOLUME_0001" TYPE="VOLUME" DMDID="MODSMD_VOLUME_0001" ADMID="RIGHTS_0001">
{doc_divs(oc_files, "      ")}{chap_divs}{suppl_div}    </mets:div>
  </mets:structMap>
</mets:mets>
"""


def open_od(ctx, od_files):
    if not od_files:
        return None
    with open(os.path.join(ctx.target, od_files[0]), "rb") as f:
        return f.read()


def build_epe_mets(ctx, psp, oc_files, od_files, articles, with_suppl):
    dmd = (perio_title(ctx) + perio_volume(ctx) + perio_issue(ctx)
           + "".join(perio_article(ctx, n) for n in range(1, articles + 1)))
    if with_suppl:
        dmd += supplement(ctx, PERIO_TITLE)
    art_divs = "".join(f'            <mets:div ID="ART_{n:04d}" TYPE="ARTICLE" DMDID="MODSMD_ART_{n:04d}" '
                       f'LABEL="Článek {n}" ORDER="{n}"/>\n' for n in range(1, articles + 1))
    suppl_div = ('            <mets:div ID="SUPPL_0001" TYPE="SUPPLEMENT" DMDID="MODSMD_SUPPL_0001" LABEL="Tabulky"/>\n'
                 if with_suppl else "")
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<mets:mets {METS_NS} LABEL="{PERIO_TITLE}, 9, 1" TYPE="{METS_TYPE['epe']}">
{mets_hdr()}{dmd}{primary_amdsec("AMD_ISSUE_0001", oc_files, od_files, open_od(ctx, od_files))}{rights(ctx)}{oc_filesec(psp, oc_files)}  <mets:structMap>
    <mets:div ID="TITLE_0001" TYPE="TITLE" DMDID="MODSMD_TITLE_0001">
      <mets:div ID="VOLUME_0001" TYPE="VOLUME" DMDID="MODSMD_VOLUME_0001" LABEL="9">
        <mets:div ID="ISSUE_0001" TYPE="ISSUE" DMDID="MODSMD_ISSUE_0001" ADMID="RIGHTS_0001" LABEL="1">
{doc_divs(oc_files, "          ")}{art_divs}{suppl_div}        </mets:div>
      </mets:div>
    </mets:div>
  </mets:structMap>
</mets:mets>
"""


def build_sep_mets(ctx, psp, oc_files, od_files, put):
    """Skládané e-periodikum 1.3, varianta A: každý článek má archivní kopii a vedlejší METS v amdsec (kap. 5.3)"""
    with_amdsec = "missing-amdsec" not in ctx.mutations
    od_data = open_od(ctx, od_files)
    amds = []
    if with_amdsec:
        for n, (rel, data, ext) in enumerate(oc_files, 1):
            oc_name = os.path.basename(rel)
            if n == 1 and od_data is not None:
                objs = premis_object("OBJ_001", "od_0001", "bit-level", od_data, "od", os.path.basename(od_files[0]),
                                     events=["event_001"])
                objs += premis_object("OBJ_002", "oc_0001", "logical preservation", data, ext, oc_name, rel="od_0001",
                                      events=["event_001"])
                evs = premis_event("EVT_001", "event_001", "migration", "agent_001", ["od_0001", "oc_0001"])
                admid = "OBJ_002"
            else:
                objs = premis_object("OBJ_001", f"oc_{n:04d}", "logical preservation", data, ext, oc_name,
                                     events=["event_001"])
                evs = premis_event("EVT_001", "event_001", "E-born creation", "agent_001", [f"oc_{n:04d}"])
                admid = "OBJ_001"
            ag = premis_agent("AGENT_001", "agent_001", "GPL Ghostscript 10.0")
            amd = f"""<?xml version="1.0" encoding="UTF-8"?>
<mets:mets {METS_NS} LABEL="{PERIO_TITLE}, 9, 1" TYPE="{METS_TYPE['sep']}">
{mets_hdr()}  <mets:amdSec ID="AMD_METS_{n:04d}">
{objs}{evs}{ag}  </mets:amdSec>
  <mets:fileSec>
    <mets:fileGrp ID="OC_EBSFL" USE="master">
{file_entry(f"oc_{psp}_{n:04d}", "/" + rel, data, FORMAT_INFO[ext][0], 1, admid=admid)}    </mets:fileGrp>
  </mets:fileSec>
  <mets:structMap TYPE="PHYSICAL" LABEL="Physical_Structure">
    <mets:div ID="DIV_P_ART_{n:04d}" TYPE="EPERIODICAL_ARTICLE" ORDER="{n}">
      <mets:fptr FILEID="oc_{psp}_{n:04d}"/>
    </mets:div>
  </mets:structMap>
</mets:mets>
"""
            amd_rel = f"amdsec/amd_mets_{psp}_{n:04d}.xml"
            amds.append((amd_rel, put(amd_rel, amd)))

    articles = len(oc_files)
    dmd = (perio_title(ctx) + perio_volume(ctx) + perio_issue(ctx)
           + "".join(perio_article(ctx, n) for n in range(1, articles + 1)))
    grp = "OC_EBGRP" if "oc-filegrp-ebgrp" in ctx.mutations else "OC_EBSFL"
    fs = f'  <mets:fileSec>\n    <mets:fileGrp ID="{grp}" USE="master">\n'
    fs += "".join(file_entry(f"oc_{psp}_{n:04d}", "/" + rel, data, FORMAT_INFO[ext][0], n)
                  for n, (rel, data, ext) in enumerate(oc_files, 1))
    fs += "    </mets:fileGrp>\n"
    if amds:
        fs += '    <mets:fileGrp ID="TECHMDGRP" USE="Technical Metadata">\n'
        fs += "".join(file_entry(f"amd_mets_{psp}_{n:04d}", "/" + rel, data, "text/xml", n)
                      for n, (rel, data) in enumerate(amds, 1))
        fs += "    </mets:fileGrp>\n"
    fs += "  </mets:fileSec>\n"
    types = [article_type(ctx, n) for n in range(1, articles + 1)]
    phys = "".join(f'      <mets:div ID="DIV_P_ART_{n:04d}" TYPE="{types[n - 1]}" LABEL="Článek {n}" ORDER="{n}">\n'
                   f'        <mets:fptr FILEID="oc_{psp}_{n:04d}"/>\n'
                   + (f'        <mets:fptr FILEID="amd_mets_{psp}_{n:04d}"/>\n' if amds else "")
                   + "      </mets:div>\n" for n in range(1, articles + 1))
    logi = "".join(f"""            <mets:div ID="ART_{n:04d}" TYPE="ARTICLE" DMDID="MODSMD_ART_{n:04d}" LABEL="Článek {n}" ORDER="{n}">
              <mets:div ID="DOCUMENT_{n:04d}" TYPE="DOCUMENT" LABEL="oc_{psp}_{n:04d}">
                <mets:div ID="FILE_{n:04d}" TYPE="FILE" LABEL="oc_{psp}_{n:04d}">
                  <mets:fptr FILEID="oc_{psp}_{n:04d}"/>
                </mets:div>
              </mets:div>
            </mets:div>
""" for n in range(1, articles + 1))
    links = "".join(f'    <mets:smLink xlink:from="ART_{n:04d}" xlink:to="DIV_P_ART_{n:04d}"/>\n'
                    for n in range(1, articles + 1))
    rights_sec = rights(ctx).replace('ID="AMD_RIGHTS_0001"', 'ID="AMD_ISSUE_0001"')
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<mets:mets {METS_NS} LABEL="{PERIO_TITLE}, 9, 1" TYPE="{METS_TYPE['sep']}">
{mets_hdr()}{dmd}{rights_sec}{fs}  <mets:structMap TYPE="PHYSICAL" LABEL="Physical_Structure">
    <mets:div ID="DIV_P_ISSUE_0001" TYPE="electronic_coll_journal" LABEL="{PERIO_TITLE}" DMDID="MODSMD_ISSUE_0001" ADMID="RIGHTS_0001">
{phys}    </mets:div>
  </mets:structMap>
  <mets:structMap TYPE="LOGICAL" LABEL="Logical_Structure">
    <mets:div ID="TITLE_0001" TYPE="PERIODICAL_TITLE" DMDID="MODSMD_TITLE_0001" LABEL="{PERIO_TITLE}" ADMID="RIGHTS_0001">
      <mets:div ID="VOLUME_0001" TYPE="PERIODICAL_VOLUME" DMDID="MODSMD_VOLUME_0001" LABEL="9">
        <mets:div ID="ISSUE_0001" TYPE="ISSUE" DMDID="MODSMD_ISSUE_0001" LABEL="1">
{logi}        </mets:div>
      </mets:div>
    </mets:div>
  </mets:structMap>
  <mets:structLink>
{links}  </mets:structLink>
</mets:mets>
"""


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("target", nargs="?", help="cílový adresář balíčku; jeho název je PSP-ID")
    ap.add_argument("--type", choices=("emo", "epe", "sep"))
    ap.add_argument("--version", help="verze DMF (fDMF), např. 3.1")
    ap.add_argument("--format", choices=("pdf", "epub"), default="pdf", help="formát archivní kopie")
    ap.add_argument("--standard", choices=("aacr", "rda"), default="rda", help="popis podle AACR2 nebo RDA")
    ap.add_argument("--chapters", type=int, default=1, help="(emo) počet kapitol")
    ap.add_argument("--articles", type=int, default=2, help="(epe, sep) počet článků; u sep i počet archivních kopií")
    ap.add_argument("--with-supplement", action="store_true", help="(emo, epe) příloha (dmdSec SUPPL)")
    ap.add_argument("--with-originaldata", action="store_true", help="(emo 3.1, epe 2.6, sep 1.3) složka originaldata")
    ap.add_argument("--mutation", action="append", default=[], help="název mutace, viz --list-mutations")
    ap.add_argument("--list-mutations", action="store_true")
    ap.add_argument("--pdf", default=DEFAULT_PDF, help="PDF/A pro archivní kopie (default dummy_PDFA-1B.pdf)")
    ap.add_argument("--epub", default=DEFAULT_EPUB, help="EPUB pro archivní kopie (default synteticky_epub3.epub)")
    ap.add_argument("--od", default=DEFAULT_OD, help="původní nearchivní PDF pro originaldata (default synteticky_bez_pdfa.pdf)")
    a = ap.parse_args()
    if a.list_mutations:
        for k, v in MUTATIONS.items():
            print(f"{k}: {v}")
        return
    if not a.target or not a.type or not a.version:
        ap.error("chybí cílový adresář, --type nebo --version")
    generate(a.target, a.type, a.version, a.format, a.standard, a.chapters, a.articles, a.with_supplement,
             a.with_originaldata, a.mutation, a.pdf, a.epub, a.od)
    print("OK:", a.target)


if __name__ == "__main__":
    main()
