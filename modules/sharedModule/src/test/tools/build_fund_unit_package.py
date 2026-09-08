#!/usr/bin/env python3
"""
Sestaveni SIP balicku podle DMF Jednotky fondu 0.1 z realnych skenu (JP2) a popisnych metadat v JSON predpisu.

Doplnuje gen_fund_unit_package.py (synteticky generator): pouziva jeho stavebni bloky pro PREMIS, METS a MODS,
ale obrazy, OCR a technicka metadata vychazeji ze skutecnych souboru:
  - mastercopy = dodany JP2 (MC), usercopy = ztratovy JP2 vyrobeny Kakadu (kdu_expand + kdu_compress),
  - ALTO + TXT = tesseract (ALTO 3.0, namespace ns-v3#) nad obrazem dekodovanym opj_decompress,
  - MIX = hodnoty prectene z hlavicky a kodoveho proudu JP2 (rozmery, barvy, komprese, dlazdice, vrstvy, urovne),
  - PREMIS objekt PS (puvodni sken) = dodany archivni sken (AC), pokud je v predpisu uveden; v balicku neni (deleted).

Pouziti:
    python3 build_fund_unit_package.py <predpis.json> <vystupni_adresar> [--unit PSP]... [--skip-ocr] [--no-uc]
                                       [--kakadu-bin DIR] [--kakadu-lib DIR] [--lang ces]

Predpis (JSON), viz fund_unit_spec_example.json vedle tohoto skriptu:
  {
    "sigla": "ABG001", "institution": "...", "created": "2026-09-08T10:00:00", "standard": "rda",
    "source_root": "/cesta/ke/skenum",
    "agents": {"scanner": {...}, "processing": {...}},           # volitelne, viz DEFAULT_AGENTS
    "collection": {"uuid": ..., "title": ..., ...},              # UNITCOLLECTION (spolecna pro vsechny jednotky)
    "units": [ {"psp": "<uuid>", "type": "clipping", "uuid": ..., "urnnbn": ..., "title": ...,
                "directory": {...} | null, "pages": [{"mc": "rel/cesta.jp2", "ac": "rel/cesta.jp2", "type": "normalPage",
                "pageNumber": "84", "alto": "rel/hotove.alto.xml", "txt": "rel/hotove.txt"}], ...} ]
  Volitelna pole strany alto/txt: prevzit hotove OCR misto tesseractu (agent OCR pak nastavit v agents.ocr,
  datum OCR eventu v "ocr_created").
  }
Kazda jednotka = jeden SIP balicek <vystupni_adresar>/<psp>. Mezivysledky (PNG, OCR) jsou v <vystupni_adresar>/.work.
"""
import argparse
import json
import os
import re
import shutil
import struct
import subprocess
import sys
import xml.etree.ElementTree as ET
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_fund_unit_package as gen  # noqa: E402

KAKADU_BIN_DEFAULT = os.path.expanduser("~/Software/kakadu/latest/bin/Mac-arm-64-gcc")
KAKADU_LIB_DEFAULT = os.path.expanduser("~/Software/kakadu/latest/lib/Mac-arm-64-gcc")
ALTO_NS = "http://www.loc.gov/standards/alto/ns-v3#"

TYPES = {
    # typ jednotky -> (METS @TYPE, mods:genre, dc:type navic)
    "clipping": ("Clipping", "clipping"),
    "clippingIndex": ("Clipping index", "clippingIndex"),
    "cardIndex": ("Card index", "cardIndex"),
}

DEFAULT_AGENTS = {
    "scanner": {"id": "scanner", "name": "skener (typ neuveden)", "type": "hardware", "version": "neuvedeno"},
    "processing": {"id": "postprocessing", "name": "zpracování obrazu (nástroj neuveden)", "type": "software", "version": "neuvedeno"},
    "kakadu": {"id": "Kakadu-8.5", "name": "Kakadu kdu_compress", "type": "software", "version": "8.5"},
    "ocr": {"id": "tesseract-5.5.3", "name": "Tesseract OCR", "type": "software", "version": "5.5.3"},
}


def esc(s) -> str:
    return (str(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")) if s is not None else ""


def run(cmd, env=None):
    r = subprocess.run(cmd, capture_output=True, text=True, env=env)
    if r.returncode != 0:
        sys.exit(f"příkaz selhal ({r.returncode}): {' '.join(cmd)}\n{r.stderr[-2000:]}")
    return r


# ----------------------------------------------------------------------------------------------------
# JP2: hlavicka a kodovy proud

def _boxes(b, start, end):
    p = start
    while p + 8 <= end:
        (length,) = struct.unpack(">I", b[p:p + 4])
        box_type = b[p + 4:p + 8]
        hdr = 8
        if length == 1:
            (length,) = struct.unpack(">Q", b[p + 8:p + 16])
            hdr = 16
        if length == 0:
            length = end - p
        yield box_type, p + hdr, p + length
        p += length


def jp2_props(path: str) -> dict:
    """Rozmery, pocet slozek, bitova hloubka, barevny prostor, rozliseni a parametry kodoveho proudu."""
    b = open(path, "rb").read()
    info = {"size": len(b), "md5": gen.md5_bytes(b)}
    for t, s, e in _boxes(b, 0, len(b)):
        if t == b"jp2h":
            for t2, s2, e2 in _boxes(b, s, e):
                if t2 == b"ihdr":
                    h, w, nc, bpc = struct.unpack(">IIHB", b[s2:s2 + 11])
                    info.update(width=w, height=h, components=nc, bits=(bpc & 0x7F) + 1)
                elif t2 == b"colr":
                    if b[s2] == 1:
                        info["enumcs"] = struct.unpack(">I", b[s2 + 3:s2 + 7])[0]
                    else:
                        info["icc"] = True
                elif t2 == b"res ":
                    for t3, s3, e3 in _boxes(b, s2, e2):
                        if t3 in (b"resc", b"resd"):
                            vn, vd, hn, hd, ve, he = struct.unpack(">HHHHbb", b[s3:s3 + 10])
                            # pixely na metr -> dpi
                            info["dpi_x"] = round(hn / hd * 10 ** he * 0.0254)
                            info["dpi_y"] = round(vn / vd * 10 ** ve * 0.0254)
        elif t == b"jp2c":
            cs = b[s:e]
            p = 2  # SOC
            while p + 4 <= len(cs):
                marker = cs[p:p + 2]
                (ln,) = struct.unpack(">H", cs[p + 2:p + 4])
                seg = cs[p + 4:p + 2 + ln]
                if marker == b"\xff\x51":  # SIZ
                    xsiz, ysiz, xo, yo, xt, yt, xto, yto, csiz = struct.unpack(">IIIIIIIIH", seg[2:36])
                    info.update(tile_w=min(xt, xsiz), tile_h=min(yt, ysiz))
                elif marker == b"\xff\x52":  # COD
                    (layers,) = struct.unpack(">H", seg[2:4])
                    info.update(layers=layers, levels=seg[5], reversible=(seg[9] == 1))
                elif marker == b"\xff\x64":  # COM
                    info.setdefault("comments", []).append(seg[2:].decode("latin1"))
                elif marker == b"\xff\x90":  # SOT: konec hlavicky
                    break
                p += 2 + ln
    codec = next((c for c in info.get("comments", []) if c.startswith("Kakadu")), None)
    info["codec"] = "Kakadu" if codec else "unknown"
    info["codec_version"] = codec.split("-v")[1] if codec and "-v" in codec else "unknown"
    return info


def color_space(props: dict) -> str:
    if props.get("components") == 1:
        return "greyscale"
    return {16: "sRGB", 17: "greyscale", 18: "sYCC"}.get(props.get("enumcs"), "RGB" if props.get("components") == 3 else "other")


# ----------------------------------------------------------------------------------------------------
# MIX z realnych hodnot (struktura shodna se synthetickym generatorem, hodnoty z jp2_props)

def mix_common_head(mix_id: str, ident: str, fmt: str, compression: str, props: dict) -> str:
    return f'''
            <mets:techMD ID="{mix_id}">
                <mets:mdWrap MDTYPE="NISOIMG" MIMETYPE="text/xml">
                    <mets:xmlData>
                        <mix:mix>
                            <mix:BasicDigitalObjectInformation>
                                <mix:ObjectIdentifier>
                                    <mix:objectIdentifierType>local</mix:objectIdentifierType>
                                    <mix:objectIdentifierValue>{ident}</mix:objectIdentifierValue>
                                </mix:ObjectIdentifier>
                                <mix:fileSize>{props["size"]}</mix:fileSize>
                                <mix:FormatDesignation>
                                    <mix:formatName>{fmt}</mix:formatName>
                                    <mix:formatVersion>1.0</mix:formatVersion>
                                </mix:FormatDesignation>
                                <mix:byteOrder>big endian</mix:byteOrder>
                                <mix:Compression>
                                    <mix:compressionScheme>{compression}</mix:compressionScheme>
                                </mix:Compression>
                            </mix:BasicDigitalObjectInformation>'''


def mix_assessment(props: dict) -> str:
    dpi = props.get("dpi_x")
    freq = (f'''
                                    <mix:xSamplingFrequency>
                                        <mix:numerator>{props["dpi_x"]}</mix:numerator>
                                        <mix:denominator>1</mix:denominator>
                                    </mix:xSamplingFrequency>
                                    <mix:ySamplingFrequency>
                                        <mix:numerator>{props["dpi_y"]}</mix:numerator>
                                        <mix:denominator>1</mix:denominator>
                                    </mix:ySamplingFrequency>''' if dpi else "")
    bits = "".join(f"\n                                        <mix:bitsPerSampleValue>{props['bits']}</mix:bitsPerSampleValue>"
                   for _ in range(props["components"]))
    return f'''
                            <mix:ImageAssessmentMetadata>
                                <mix:SpatialMetrics>
                                    <mix:samplingFrequencyUnit>{"in." if dpi else "no absolute unit of measurement"}</mix:samplingFrequencyUnit>{freq}
                                </mix:SpatialMetrics>
                                <mix:ImageColorEncoding>
                                    <mix:BitsPerSample>{bits}
                                        <mix:bitsPerSampleUnit>integer</mix:bitsPerSampleUnit>
                                    </mix:BitsPerSample>
                                    <mix:samplesPerPixel>{props["components"]}</mix:samplesPerPixel>
                                </mix:ImageColorEncoding>
                            </mix:ImageAssessmentMetadata>'''


def mix_ps_real(ident: str, props: dict, captured: str, producer: str, scanner: dict) -> str:
    scanner_el = ""
    if scanner.get("manufacturer"):
        scanner_el = f'''
                                <mix:ScannerCapture>
                                    <mix:scannerManufacturer>{esc(scanner["manufacturer"])}</mix:scannerManufacturer>
                                    <mix:ScannerModel>
                                        <mix:scannerModelName>{esc(scanner.get("model", "neuvedeno"))}</mix:scannerModelName>
                                        <mix:scannerModelNumber>{esc(scanner.get("model_number", "neuvedeno"))}</mix:scannerModelNumber>
                                        <mix:scannerModelSerialNo>{esc(scanner.get("serial", "neuvedeno"))}</mix:scannerModelSerialNo>
                                    </mix:ScannerModel>
                                    <mix:MaximumOpticalResolution>
                                        <mix:xOpticalResolution>{scanner.get("optical_dpi", props.get("dpi_x", 300))}</mix:xOpticalResolution>
                                        <mix:yOpticalResolution>{scanner.get("optical_dpi", props.get("dpi_y", 300))}</mix:yOpticalResolution>
                                        <mix:opticalResolutionUnit>in.</mix:opticalResolutionUnit>
                                    </mix:MaximumOpticalResolution>
                                    <mix:scannerSensor>{esc(scanner.get("sensor", "undefined"))}</mix:scannerSensor>
                                    <mix:ScanningSystemSoftware>
                                        <mix:scanningSoftwareName>{esc(scanner.get("software", "neuvedeno"))}</mix:scanningSoftwareName>
                                        <mix:scanningSoftwareVersionNo>{esc(scanner.get("software_version", "neuvedeno"))}</mix:scanningSoftwareVersionNo>
                                    </mix:ScanningSystemSoftware>
                                </mix:ScannerCapture>'''
    return mix_common_head("MIX_001", ident, "image/jp2", "JPEG 2000 Lossless" if props["reversible"] else "JPEG 2000 Lossy", props) + f'''
                            <mix:BasicImageInformation>
                                <mix:BasicImageCharacteristics>
                                    <mix:imageWidth>{props["width"]}</mix:imageWidth>
                                    <mix:imageHeight>{props["height"]}</mix:imageHeight>
                                    <mix:PhotometricInterpretation>
                                        <mix:colorSpace>{color_space(props)}</mix:colorSpace>
                                    </mix:PhotometricInterpretation>
                                </mix:BasicImageCharacteristics>
                            </mix:BasicImageInformation>
                            <mix:ImageCaptureMetadata>
                                <mix:GeneralCaptureInformation>
                                    <mix:dateTimeCreated>{captured}</mix:dateTimeCreated>
                                    <mix:imageProducer>{esc(producer)}</mix:imageProducer>
                                    <mix:captureDevice>reflection print scanner</mix:captureDevice>
                                </mix:GeneralCaptureInformation>{scanner_el}
                                <mix:orientation>normal*</mix:orientation>
                            </mix:ImageCaptureMetadata>''' + mix_assessment(props) + '''
                        </mix:mix>
                    </mets:xmlData>
                </mets:mdWrap>
            </mets:techMD>'''


def mix_mc_real(ident: str, props: dict, processed: str, source_name: str) -> str:
    return mix_common_head("MIX_002", ident, "image/jp2", "JPEG 2000 Lossless" if props["reversible"] else "JPEG 2000 Lossy", props) + f'''
                            <mix:BasicImageInformation>
                                <mix:BasicImageCharacteristics>
                                    <mix:imageWidth>{props["width"]}</mix:imageWidth>
                                    <mix:imageHeight>{props["height"]}</mix:imageHeight>
                                    <mix:PhotometricInterpretation>
                                        <mix:colorSpace>{color_space(props)}</mix:colorSpace>
                                    </mix:PhotometricInterpretation>
                                </mix:BasicImageCharacteristics>
                                <mix:SpecialFormatCharacteristics>
                                    <mix:JPEG2000>
                                        <mix:CodecCompliance>
                                            <mix:codec>{props["codec"]}</mix:codec>
                                            <mix:codecVersion>{props["codec_version"]}</mix:codecVersion>
                                            <mix:codestreamProfile>P1</mix:codestreamProfile>
                                            <mix:complianceClass>C1</mix:complianceClass>
                                        </mix:CodecCompliance>
                                        <mix:EncodingOptions>
                                            <mix:Tiles>
                                                <mix:tileWidth>{props["tile_w"]}</mix:tileWidth>
                                                <mix:tileHeight>{props["tile_h"]}</mix:tileHeight>
                                            </mix:Tiles>
                                            <mix:qualityLayers>{props["layers"]}</mix:qualityLayers>
                                            <mix:resolutionLevels>{props["levels"]}</mix:resolutionLevels>
                                        </mix:EncodingOptions>
                                    </mix:JPEG2000>
                                </mix:SpecialFormatCharacteristics>
                            </mix:BasicImageInformation>''' + mix_assessment(props) + f'''
                            <mix:ChangeHistory>
                                <mix:ImageProcessing>
                                    <mix:dateTimeProcessed>{processed}</mix:dateTimeProcessed>
                                    <mix:sourceData>{esc(source_name)}</mix:sourceData>
                                </mix:ImageProcessing>
                            </mix:ChangeHistory>
                        </mix:mix>
                    </mets:xmlData>
                </mets:mdWrap>
            </mets:techMD>'''


# ----------------------------------------------------------------------------------------------------
# amd_mets pro jednu stranu

def amd_mets_real(psp: str, mets_type: str, label: str, page_no: int, files: dict, mc_props: dict, ac: dict,
                  spec: dict, agents: dict, created: str) -> str:
    p = f"{page_no:04d}"
    ps_id, mc_id, alto_id = f"PS_{psp}_{p}", f"MC_{psp}_{p}", f"XML_{psp}_{p}"
    ev_capture, ev_delete, ev_mc, ev_alto, ev_txt = f"capture_{p}", f"deletion_{p}", f"mc_creation_{p}", f"alto_creation_{p}", f"txt_creation_{p}"
    mc, uc, alto, txt = files["mc"], files["uc"], files["alto"], files["txt"]
    captured = spec.get("captured", created)
    ag = agents
    # PS = archivni sken (AC), pokud je; jinak se popise samotny MC jako puvodni sken (bez odvozeni)
    if ac:
        objects = gen.premis_object("OBJ_001", ps_id, "deleted", ac["props"]["size"], ac["props"]["md5"], "image/jp2", "1.0",
                                    "x-fmt/392", ag["scanner"]["name"], ag["scanner"]["version"], ac["name"], linking_events=(ev_capture, ev_delete))
        objects += gen.premis_object("OBJ_002", mc_id, "preservation", mc[1], mc[2], "image/jp2", "1.0", "x-fmt/392",
                                     ag["processing"]["name"], ag["processing"]["version"], os.path.basename(mc[0]),
                                     relationship=gen.premis_relationship(ps_id, ev_mc), linking_events=(ev_mc,))
        mixes = mix_ps_real(ps_id, ac["props"], captured, spec["institution"], ag.get("scanner_mix", {})) \
            + mix_mc_real(mc_id, mc_props, captured, ac["name"])
        events = (gen.premis_event("EVT_001", ev_capture, "capture", "capture/digitization", ag["scanner"]["id"], "hardware", ps_id)
                  + gen.premis_event("EVT_002", ev_delete, "deletion", "deletion/PS_deletion", ag["processing"]["id"], "software", ps_id)
                  + gen.premis_event("EVT_003", ev_mc, "migration", "migration/MC_creation", ag["processing"]["id"], "software", mc_id))
    else:
        objects = gen.premis_object("OBJ_002", mc_id, "preservation", mc[1], mc[2], "image/jp2", "1.0", "x-fmt/392",
                                    ag["scanner"]["name"], ag["scanner"]["version"], os.path.basename(mc[0]), linking_events=(ev_capture,))
        mixes = mix_mc_real(mc_id, mc_props, captured, os.path.basename(mc[0]))
        events = gen.premis_event("EVT_001", ev_capture, "capture", "capture/digitization", ag["scanner"]["id"], "hardware", mc_id)
    objects += gen.premis_object("OBJ_003", alto_id, "preservation", alto[1], alto[2], "text/xml", "1.0", "fmt/101",
                                 ag["ocr"]["name"], ag["ocr"]["version"], os.path.basename(alto[0]),
                                 relationship=gen.premis_relationship(mc_id, ev_alto))
    # OCR eventy mohou nest jine datum nez sestaveni balicku (prevzate ALTO z drivejsiho zpracovani: spec["ocr_created"])
    saved_created = gen.CREATED
    gen.CREATED = spec.get("ocr_created", created)
    try:
        events += (gen.premis_event("EVT_004", ev_alto, "capture", "capture/XML_creation", ag["ocr"]["id"], "software", alto_id)
                   + gen.premis_event("EVT_005", ev_txt, "capture", "capture/TXT_creation", ag["ocr"]["id"], "software", alto_id))
    finally:
        gen.CREATED = saved_created
    agents_xml = (gen.premis_agent("AGENT_001", ag["scanner"]["id"], ag["scanner"]["name"], ag["scanner"]["type"])
                  + gen.premis_agent("AGENT_002", ag["processing"]["id"], ag["processing"]["name"], ag["processing"]["type"])
                  + gen.premis_agent("AGENT_003", ag["kakadu"]["id"], ag["kakadu"]["name"], ag["kakadu"]["type"])
                  + gen.premis_agent("AGENT_004", ag["ocr"]["id"], ag["ocr"]["name"], ag["ocr"]["type"]))

    def file_el(fid, rel, size, digest, mime, admid=""):
        adm = f' ADMID="{admid}"' if admid else ""
        return f'''
            <mets:file ID="{fid}" MIMETYPE="{mime}" SIZE="{size}" CHECKSUMTYPE="MD5" CHECKSUM="{digest}" SEQ="{page_no - 1}" CREATED="{created}"{adm}>
                <mets:FLocat LOCTYPE="URL" xlink:href="{rel}"/>
            </mets:file>'''

    return f'''<?xml version="1.0" encoding="UTF-8"?>
<mets:mets {gen.METS_NS} LABEL="{esc(label)}" TYPE="{mets_type}" xsi:schemaLocation="{gen.SCHEMA_LOCATION}">
    <mets:metsHdr CREATEDATE="{created}" LASTMODDATE="{created}">
        <mets:agent ROLE="CREATOR" TYPE="ORGANIZATION">
            <mets:name>{spec["sigla"]}</mets:name>
        </mets:agent>
        <mets:agent ROLE="ARCHIVIST" TYPE="ORGANIZATION">
            <mets:name>{spec["sigla"]}</mets:name>
        </mets:agent>
    </mets:metsHdr>
    <mets:amdSec ID="PAGE{p}">{objects}{mixes}{events}{agents_xml}
    </mets:amdSec>
    <mets:fileSec>
        <mets:fileGrp ID="MC_IMGGRP" USE="Images">{file_el(f"MC_{p}", *mc, "image/jp2", "OBJ_002 MIX_002")}
        </mets:fileGrp>
        <mets:fileGrp ID="UC_IMGGRP" USE="Images">{file_el(f"UC_{p}", *uc, "image/jp2")}
        </mets:fileGrp>
        <mets:fileGrp ID="ALTOGRP" USE="Layout">{file_el(f"ALTO_{p}", *alto, "text/xml", "OBJ_003")}
        </mets:fileGrp>
        <mets:fileGrp ID="TXTGRP" USE="Text">{file_el(f"TXT_{p}", *txt, "text/plain")}
        </mets:fileGrp>
    </mets:fileSec>
    <mets:structMap TYPE="PHYSICAL" LABEL="Physical_Structure">
        <mets:div TYPE="UNIT" ID="DIV_UNIT_{p}" LABEL="{esc(label)}">
            <mets:fptr FILEID="MC_{p}"/>
            <mets:fptr FILEID="UC_{p}"/>
            <mets:fptr FILEID="ALTO_{p}"/>
            <mets:fptr FILEID="TXT_{p}"/>
        </mets:div>
    </mets:structMap>
</mets:mets>
'''


# ----------------------------------------------------------------------------------------------------
# MODS / DC z predpisu

def mods_names(names: list) -> str:
    out = ""
    for n in names or []:
        ntype = n.get("type", "personal")
        parts = ""
        if ntype == "personal":
            if n.get("family"):
                parts += f'\n                            <mods:namePart type="family">{esc(n["family"])}</mods:namePart>'
            if n.get("given"):
                parts += f'\n                            <mods:namePart type="given">{esc(n["given"])}</mods:namePart>'
            if n.get("date"):
                parts += f'\n                            <mods:namePart type="date">{esc(n["date"])}</mods:namePart>'
        else:
            parts += f'\n                            <mods:namePart>{esc(n["name"])}</mods:namePart>'
        usage = ' usage="primary"' if n.get("primary") else ""
        out += f'''
                        <mods:name type="{ntype}"{usage}>{parts}
                            <mods:role>
                                <mods:roleTerm type="code" authority="marcrelator">{n.get("role", "aut")}</mods:roleTerm>
                            </mods:role>
                        </mods:name>'''
    return out


def dc_creators(names: list) -> str:
    out = ""
    for n in names or []:
        label = f'{n["family"]}, {n["given"]}' if n.get("family") and n.get("given") else n.get("family") or n.get("name", "")
        out += f"\n                    <dc:creator>{esc(label)}</dc:creator>"
    return out


def mods_identifiers(node: dict) -> str:
    out = f'\n                        <mods:identifier type="uuid">{node["uuid"]}</mods:identifier>'
    if node.get("urnnbn"):
        out += f'\n                        <mods:identifier type="urnnbn">{node["urnnbn"]}</mods:identifier>'
    for t, v in (node.get("identifiers") or {}).items():
        out += f'\n                        <mods:identifier type="{t}">{esc(v)}</mods:identifier>'
    return out


def dc_identifiers(node: dict) -> str:
    out = f'\n                    <dc:identifier>uuid:{node["uuid"]}</dc:identifier>'
    if node.get("urnnbn"):
        out += f'\n                    <dc:identifier>urnnbn:{node["urnnbn"]}</dc:identifier>'
    for t, v in (node.get("identifiers") or {}).items():
        out += f"\n                    <dc:identifier>{t}:{esc(v)}</dc:identifier>"
    return out


def mods_dates(dates: dict) -> str:
    if not dates:
        return ""
    if dates.get("start") and dates.get("end"):
        q = ' qualifier="approximate"' if dates.get("approximate") else ""
        return f'''
                        <mods:originInfo>
                            <mods:dateCreated point="start"{q}>{esc(dates["start"])}</mods:dateCreated>
                            <mods:dateCreated point="end"{q}>{esc(dates["end"])}</mods:dateCreated>
                        </mods:originInfo>'''
    if dates.get("date"):
        return f'''
                        <mods:originInfo>
                            <mods:dateCreated>{esc(dates["date"])}</mods:dateCreated>
                        </mods:originInfo>'''
    return ""


def dc_date(dates: dict) -> str:
    if not dates:
        return ""
    if dates.get("start") and dates.get("end"):
        return f'\n                    <dc:date>{esc(dates["start"])}-{esc(dates["end"])}</dc:date>'
    return f'\n                    <dc:date>{esc(dates["date"])}</dc:date>' if dates.get("date") else ""


def mods_subjects(subjects: list) -> str:
    out = ""
    for s in subjects or []:
        inner = "".join(f'\n                            <mods:{k}>{esc(v)}</mods:{k}>' for k, v in s.items())
        out += f'\n                        <mods:subject authority="czenas">{inner}\n                        </mods:subject>'
    return out


def dc_subjects(subjects: list) -> str:
    return "".join(f"\n                    <dc:subject>{esc(v)}</dc:subject>" for s in subjects or [] for v in s.values())


def title_block(node: dict) -> str:
    sub = f'\n                            <mods:subTitle>{esc(node["subtitle"])}</mods:subTitle>' if node.get("subtitle") else ""
    pn = f'\n                            <mods:partNumber>{esc(node["partNumber"])}</mods:partNumber>' if node.get("partNumber") else ""
    return f'''
                        <mods:titleInfo>
                            <mods:title>{esc(node["title"])}</mods:title>{sub}{pn}
                        </mods:titleInfo>'''


def dc_title(node: dict) -> str:
    t = node["title"]
    if node.get("subtitle"):
        t += " : " + node["subtitle"]
    if node.get("partNumber"):
        t += ". " + str(node["partNumber"])
    return t


def phys(node: dict, extent_default: str) -> str:
    note = f'\n                            <mods:note>{esc(node["physicalNote"])}</mods:note>' if node.get("physicalNote") else ""
    return f'''
                        <mods:physicalDescription>
                            <mods:extent>{esc(node.get("extent", extent_default))}</mods:extent>{note}
                        </mods:physicalDescription>'''


def note(node: dict) -> str:
    return f'\n                        <mods:note>{esc(node["note"])}</mods:note>' if node.get("note") else ""


def location(node: dict) -> str:
    return gen.mods_location(esc(node["shelf"])) if node.get("shelf") else f'''
                        <mods:location>
                            <mods:physicalLocation authority="siglaADR">{gen.SIGLA}</mods:physicalLocation>
                        </mods:location>'''


def dc_source(node: dict) -> str:
    out = f"\n                    <dc:source>{gen.SIGLA}</dc:source>"
    if node.get("shelf"):
        out += f"\n                    <dc:source>{esc(node['shelf'])}</dc:source>"
    return out


def mods_collection(c: dict, standard: str) -> str:
    return gen.dmdsec_mods("MODSMD_UNITCOLLECTION_0001", f'''
                    <mods:mods ID="MODS_UNITCOLLECTION_0001" version="3.8">{title_block(c)}{mods_names(c.get("names"))}
                        <mods:genre>unit collection</mods:genre>{mods_dates(c.get("dates"))}{phys(c, "1 složka") if c.get("extent") else ""}{note(c)}{mods_subjects(c.get("subjects"))}{mods_identifiers(c)}{location(c)}{gen.mods_record_info(standard)}
                    </mods:mods>''')


def dc_collection(c: dict) -> str:
    fmt = f'\n                    <dc:format>{esc(c["extent"])}</dc:format>' if c.get("extent") else ""
    desc = f'\n                    <dc:description>{esc(c["note"])}</dc:description>' if c.get("note") else ""
    return gen.dmdsec_dc("DCMD_UNITCOLLECTION_0001", f'''
                    <dc:title>{esc(dc_title(c))}</dc:title>{dc_creators(c.get("names"))}
                    <dc:type>model:unitcollection</dc:type>{dc_date(c.get("dates"))}{fmt}{desc}{dc_subjects(c.get("subjects"))}{dc_identifiers(c)}{dc_source(c)}''')


def mods_directory(d: dict, standard: str) -> str:
    return gen.dmdsec_mods("MODSMD_DIRECTORY_0001", f'''
                    <mods:mods ID="MODS_DIRECTORY_0001" version="3.8">{title_block(d)}{mods_names(d.get("names"))}
                        <mods:genre>directory</mods:genre>{mods_dates(d.get("dates"))}{phys(d, "1 složka") if d.get("extent") else ""}{note(d)}{mods_subjects(d.get("subjects"))}{mods_identifiers(d)}{location(d)}{gen.mods_record_info(standard)}
                    </mods:mods>''')


def dc_directory(d: dict) -> str:
    fmt = f'\n                    <dc:format>{esc(d["extent"])}</dc:format>' if d.get("extent") else ""
    desc = f'\n                    <dc:description>{esc(d["note"])}</dc:description>' if d.get("note") else ""
    return gen.dmdsec_dc("DCMD_DIRECTORY_0001", f'''
                    <dc:title>{esc(dc_title(d))}</dc:title>{dc_creators(d.get("names"))}
                    <dc:type>model:directory</dc:type>{dc_date(d.get("dates"))}{fmt}{desc}{dc_subjects(d.get("subjects"))}{dc_identifiers(d)}{dc_source(d)}''')


def mods_host(h: dict) -> str:
    if not h:
        return ""
    place = f'''
                                <mods:place>
                                    <mods:placeTerm type="text">{esc(h["place"])}</mods:placeTerm>
                                </mods:place>''' if h.get("place") else ""
    publisher = f'''
                                <mods:agent>
                                    <mods:namePart>{esc(h["publisher"])}</mods:namePart>
                                    <mods:role>
                                        <mods:roleTerm type="text">publisher</mods:roleTerm>
                                    </mods:role>
                                </mods:agent>''' if h.get("publisher") else ""
    date = f'\n                                <mods:dateIssued>{esc(h["dateIssued"])}</mods:dateIssued>' if h.get("dateIssued") else ""
    origin = f'''
                            <mods:originInfo eventType="publication">{place}{publisher}{date}
                            </mods:originInfo>''' if (place or publisher or date) else ""
    details = ""
    for k in ("volume", "issue"):
        if h.get(k):
            details += f'''
                                <mods:detail type="{k}">
                                    <mods:number>{esc(h[k])}</mods:number>
                                </mods:detail>'''
    # profil jednotky (DMF kap. 7.3.3) v relatedItem/part pripousti jen detail type volume|issue; strany jdou do poznamky
    pages_note = f'\n                            <mods:note>s. {esc(h["pages"])}</mods:note>' if h.get("pages") else ""
    part = f'''
                            <mods:part>{details}
                            </mods:part>''' if details else ""
    ids = "".join(f'\n                            <mods:identifier type="{t}">{esc(v)}</mods:identifier>' for t, v in (h.get("identifiers") or {}).items())
    return f'''
                        <mods:relatedItem type="host">
                            <mods:titleInfo>
                                <mods:title>{esc(h["title"])}</mods:title>
                            </mods:titleInfo>{origin}{pages_note}{ids}{part}
                        </mods:relatedItem>'''


def mods_unit(u: dict, standard: str, pages: int) -> str:
    mets_type, genre = TYPES[u["type"]]
    extra_genre = f'\n                        <mods:genre>{esc(u["genre2"])}</mods:genre>' if u.get("genre2") else ""
    lang = f'''
                        <mods:language>
                            <mods:languageTerm type="code" authority="iso639-2b">{u.get("language", "cze")}</mods:languageTerm>
                        </mods:language>'''
    return gen.dmdsec_mods("MODSMD_UNIT_0001", f'''
                    <mods:mods ID="MODS_UNIT_0001" version="3.8">{title_block(u)}{mods_names(u.get("names"))}
                        <mods:genre>{genre}</mods:genre>{extra_genre}{lang}{phys(u, f"{pages} {'strana' if pages == 1 else 'strany' if pages < 5 else 'stran'}")}{note(u)}{mods_subjects(u.get("subjects"))}{mods_host(u.get("host"))}{mods_identifiers(u)}{location(u)}{gen.mods_record_info(standard)}
                    </mods:mods>''')


def dc_unit(u: dict, pages: int) -> str:
    extra = f'\n                    <dc:type>{esc(u["genre2"])}</dc:type>' if u.get("genre2") else ""
    desc = f'\n                    <dc:description>{esc(u["note"])}</dc:description>' if u.get("note") else ""
    src = f'\n                    <dc:source>{esc(u["host"]["title"])}</dc:source>' if u.get("host") else ""
    return gen.dmdsec_dc("DCMD_UNIT_0001", f'''
                    <dc:title>{esc(dc_title(u))}</dc:title>{dc_creators(u.get("names"))}
                    <dc:type>model:unit</dc:type>{extra}{dc_date(u.get("host", {}).get("dateIssued") and {"date": u["host"]["dateIssued"]})}
                    <dc:language>{u.get("language", "cze")}</dc:language>
                    <dc:format>{esc(u.get("extent", f"{pages} stran"))}</dc:format>{desc}{dc_subjects(u.get("subjects"))}{dc_identifiers(u)}{src}{dc_source(u)}''')


def mods_page(i: int, page: dict, uuid: str) -> str:
    title = f'''
                        <mods:titleInfo>
                            <mods:title>{esc(page["title"])}</mods:title>
                        </mods:titleInfo>''' if page.get("title") else ""
    number = f'''
                        <mods:part>
                            <mods:detail type="pageNumber">
                                <mods:number>{esc(page["pageNumber"])}</mods:number>
                            </mods:detail>
                        </mods:part>''' if page.get("pageNumber") else ""
    return gen.dmdsec_mods(f"MODSMD_PAGE_{i:04d}", f'''
                    <mods:mods ID="MODS_PAGE_{i:04d}" version="3.8">{title}
                        <mods:identifier type="uuid">{uuid}</mods:identifier>{number}
                        <mods:part>
                            <mods:detail type="pageIndex">
                                <mods:number>{i}</mods:number>
                            </mods:detail>
                        </mods:part>
                        <mods:typeOfResource>{page.get("resource", "text")}</mods:typeOfResource>
                    </mods:mods>''')


def dc_page(i: int, page: dict, uuid: str) -> str:
    title = f'\n                    <dc:title>{esc(page["title"])}</dc:title>' if page.get("title") else ""
    return gen.dmdsec_dc(f"DCMD_PAGE_{i:04d}", f'''{title}
                    <dc:identifier>uuid:{uuid}</dc:identifier>
                    <dc:type>{page.get("resource", "text")}</dc:type>''')


# ----------------------------------------------------------------------------------------------------
# hlavni METS

def main_mets(psp: str, spec: dict, u: dict, page_files: dict, page_uuids: dict, created: str) -> str:
    mets_type, genre = TYPES[u["type"]]
    standard = spec.get("standard", "rda")
    c, d = spec["collection"], u.get("directory")
    pages = len(u["pages"])
    label = esc(u.get("label") or u["title"])
    dmd = mods_collection(c, standard) + dc_collection(c)
    if d:
        dmd += mods_directory(d, standard) + dc_directory(d)
    dmd += mods_unit(u, standard, pages) + dc_unit(u, pages)
    for i in range(1, pages + 1):
        dmd += mods_page(i, u["pages"][i - 1], page_uuids[i]) + dc_page(i, u["pages"][i - 1], page_uuids[i])

    def grp(gid, use, key, mime):
        out = f'\n        <mets:fileGrp ID="{gid}" USE="{use}">'
        for i in range(1, pages + 1):
            rel, size, digest = page_files[i][key]
            fid = f"{key.upper()}_{i:04d}" if key != "amd" else f"AMD_METS_{i:04d}"
            out += f'''
            <mets:file ID="{fid}" MIMETYPE="{mime}" SIZE="{size}" CHECKSUMTYPE="MD5" CHECKSUM="{digest}" SEQ="{i - 1}" CREATED="{created}">
                <mets:FLocat LOCTYPE="URL" xlink:href="{rel}"/>
            </mets:file>'''
        return out + "\n        </mets:fileGrp>"

    filesec = (grp("MC_IMGGRP", "Images", "mc", "image/jp2") + grp("UC_IMGGRP", "Images", "uc", "image/jp2")
               + grp("ALTOGRP", "Layout", "alto", "text/xml") + grp("TXTGRP", "Text", "txt", "text/plain")
               + grp("TECHMDGRP", "Technical Metadata", "amd", "text/xml"))

    parts = "".join(f'\n                    <mets:div TYPE="{t}" ID="{t}_{k:04d}"/>' for k, t in enumerate(u.get("logical_parts", ["NORMAL_TEXT"]), 1))
    unit_div = f'''
                <mets:div TYPE="UNIT" ID="UNIT_0001" LABEL="{label}" DMDID="MODSMD_UNIT_0001">{parts}
                </mets:div>'''
    inner = f'''
            <mets:div TYPE="DIRECTORY" ID="DIRECTORY_0001" LABEL="{esc(d["title"])}" DMDID="MODSMD_DIRECTORY_0001">{unit_div}
            </mets:div>''' if d else unit_div
    logical = f'''
    <mets:structMap TYPE="LOGICAL" LABEL="Logical_Structure">
        <mets:div TYPE="UNITCOLLECTION" ID="UNITCOLLECTION_0001" LABEL="{esc(c["title"])}" DMDID="MODSMD_UNITCOLLECTION_0001">{inner}
        </mets:div>
    </mets:structMap>'''
    page_divs = ""
    for i in range(1, pages + 1):
        pg = u["pages"][i - 1]
        page_divs += f'''
            <mets:div TYPE="{pg.get("type", "normalPage")}" ID="DIV_PAGE_{i:04d}" ORDER="{i}" ORDERLABEL="{esc(pg.get("pageNumber", i))}" DMDID="MODSMD_PAGE_{i:04d}">
                <mets:fptr FILEID="MC_{i:04d}"/>
                <mets:fptr FILEID="UC_{i:04d}"/>
                <mets:fptr FILEID="ALTO_{i:04d}"/>
                <mets:fptr FILEID="TXT_{i:04d}"/>
                <mets:fptr FILEID="AMD_METS_{i:04d}"/>
            </mets:div>'''
    physical = f'''
    <mets:structMap TYPE="PHYSICAL" LABEL="Physical_Structure">
        <mets:div TYPE="UNIT" ID="DIV_P_0000" LABEL="{label}" DMDID="MODSMD_UNIT_0001">{page_divs}
        </mets:div>
    </mets:structMap>'''
    links = "".join(f'\n        <mets:smLink xlink:from="UNIT_0001" xlink:to="DIV_PAGE_{i:04d}"/>' for i in range(1, pages + 1))
    return f'''<?xml version="1.0" encoding="UTF-8"?>
<mets:mets {gen.METS_NS} LABEL="{label}" TYPE="{mets_type}" xsi:schemaLocation="{gen.SCHEMA_LOCATION}">
    <mets:metsHdr CREATEDATE="{created}" LASTMODDATE="{created}">
        <mets:agent ROLE="CREATOR" TYPE="ORGANIZATION">
            <mets:name>{spec["sigla"]}</mets:name>
        </mets:agent>
        <mets:agent ROLE="ARCHIVIST" TYPE="ORGANIZATION">
            <mets:name>{spec["sigla"]}</mets:name>
        </mets:agent>
    </mets:metsHdr>{dmd}
    <mets:fileSec>{filesec}
    </mets:fileSec>{logical}{physical}
    <mets:structLink>{links}
    </mets:structLink>
</mets:mets>
'''


def info_xml(psp: str, spec: dict, items: list, size_kb: int, md5_name: str, md5_digest: str, created: str) -> str:
    c = spec["collection"]
    item_els = "".join(f"\n        <item>\\{p.replace('/', chr(92))}</item>" for p in items)
    titleids = f'\n    <titleid type="uuid">{c["uuid"]}</titleid>'
    for t, v in (c.get("identifiers") or {}).items():
        titleids += f'\n    <titleid type="{t}">{esc(v)}</titleid>'
    return f'''<?xml version="1.0" encoding="UTF-8"?>
<info>
    <created>{created}</created>
    <metadataversion>0.1</metadataversion>
    <packageid>{psp}</packageid>
    <mainmets>mets_{psp}.xml</mainmets>
    <validation version="2.7">Valid</validation>{titleids}
    <collection>{esc(c["title"])}</collection>
    <institution>{esc(spec["institution"])}</institution>
    <creator>{spec["sigla"]}</creator>
    <size>{size_kb}</size>
    <itemlist itemtotal="{len(items)}">{item_els}
    </itemlist>
    <checksum type="md5" checksum="{md5_digest}">\\{md5_name}</checksum>
    <note>{esc(spec.get("note", "Vzorový balíček podle DMF Jednotky fondu 0.1 sestavený nástrojem build_fund_unit_package.py."))}</note>
</info>
'''


# ----------------------------------------------------------------------------------------------------
# obrazy a OCR

class Tools:
    def __init__(self, kakadu_bin: str, kakadu_lib: str, lang: str, work: str):
        self.kdu_expand = os.path.join(kakadu_bin, "kdu_expand")
        self.kdu_compress = os.path.join(kakadu_bin, "kdu_compress")
        self.env = dict(os.environ, DYLD_LIBRARY_PATH=kakadu_lib, LD_LIBRARY_PATH=kakadu_lib)
        self.lang = lang
        self.work = work
        os.makedirs(work, exist_ok=True)
        for tool in ("opj_decompress", "tesseract"):
            if shutil.which(tool) is None:
                sys.exit(f"nástroj {tool} není v PATH")
        if not os.path.exists(self.kdu_compress):
            sys.exit(f"Kakadu nenalezeno: {self.kdu_compress} (parametr --kakadu-bin)")

    def png(self, jp2: str) -> str:
        out = os.path.join(self.work, os.path.basename(jp2) + ".png")
        if not os.path.exists(out):
            run(["opj_decompress", "-i", jp2, "-o", out])
        return out

    def user_copy(self, mc_jp2: str) -> bytes:
        tif = os.path.join(self.work, os.path.basename(mc_jp2) + ".tif")
        uc = os.path.join(self.work, os.path.basename(mc_jp2) + ".uc.jp2")
        if not os.path.exists(uc):
            run([self.kdu_expand, "-i", mc_jp2, "-o", tif, "-quiet"], self.env)
            # ztratova uzivatelska kopie: 9/7, 1 bit/px, 5 urovni, RPCL, precinkty, jako u NDK
            run([self.kdu_compress, "-i", tif, "-o", uc, "-rate", "1.0", "Clevels=5", "Clayers=1", "Creversible=no",
                 "Corder=RPCL", "Cprecincts={256,256}", "Cblk={64,64}", "-quiet"], self.env)
            os.remove(tif)
        return open(uc, "rb").read()

    def ocr(self, mc_jp2: str, mc_name: str, created: str, agency: str, alto_src: str = None, txt_src: str = None):
        """Vrati (alto_xml_bytes, txt_bytes). Bez alto_src/txt_src: ALTO 3.0 z tesseractu s doplnenym fileName,
        processingDateTime a processingAgency. S alto_src/txt_src: prevzate soubory (napr. ALTO 2.0 od ABBYY z drivejsiho
        zpracovani); v ALTO se upravi jen sourceImageInformation/fileName, pokud element existuje."""
        if alto_src:
            alto_bytes = open(alto_src, "rb").read()
            txt_bytes = open(txt_src, "rb").read() if txt_src else b""
            root_ns = re.search(rb'<alto[^>]*\sxmlns="([^"]+)"', alto_bytes)
            if root_ns:
                ns_uri = root_ns.group(1).decode()
                ET.register_namespace("", ns_uri)
                ET.register_namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")
                ET.register_namespace("xlink", "http://www.w3.org/1999/xlink")
                root = ET.fromstring(alto_bytes)
                fn = root.find(f"{{{ns_uri}}}Description/{{{ns_uri}}}sourceImageInformation/{{{ns_uri}}}fileName")
                if fn is not None:
                    fn.text = mc_name
                    alto_bytes = b'<?xml version="1.0" encoding="UTF-8"?>\n' + ET.tostring(root, encoding="utf-8")
            return alto_bytes, txt_bytes
        base = os.path.join(self.work, os.path.basename(mc_jp2) + ".ocr")
        if not (os.path.exists(base + ".xml") and os.path.exists(base + ".txt")):
            run(["tesseract", self.png(mc_jp2), base, "-l", self.lang, "alto", "txt"])
        ET.register_namespace("", ALTO_NS)
        ET.register_namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")
        tree = ET.parse(base + ".xml")
        root = tree.getroot()
        ns = {"a": ALTO_NS}
        fn = root.find("a:Description/a:sourceImageInformation/a:fileName", ns)
        if fn is not None:
            fn.text = mc_name
        step = root.find("a:Description/a:OCRProcessing/a:ocrProcessingStep", ns)
        if step is not None and step.find("a:processingDateTime", ns) is None:
            dt = ET.Element(f"{{{ALTO_NS}}}processingDateTime")
            dt.text = created
            ag = ET.Element(f"{{{ALTO_NS}}}processingAgency")
            ag.text = agency
            step.insert(0, ag)
            step.insert(0, dt)
        alto = b'<?xml version="1.0" encoding="UTF-8"?>\n' + ET.tostring(root, encoding="utf-8")
        txt = open(base + ".txt", "rb").read()
        return alto, txt


# ----------------------------------------------------------------------------------------------------

def build_unit(spec: dict, u: dict, out_root: str, tools: Tools, no_uc: bool):
    psp = u["psp"]
    created = spec.get("created") or datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
    gen.SIGLA = spec["sigla"]
    gen.CREATED = created
    agents = {k: dict(v) for k, v in DEFAULT_AGENTS.items()}
    for k, v in (spec.get("agents") or {}).items():
        agents.setdefault(k, {}).update(v)
    mets_type, genre = TYPES[u["type"]]
    label = u.get("label") or u["title"]
    target = os.path.join(out_root, psp)
    if os.path.exists(target):
        shutil.rmtree(target)
    for d in ("mastercopy", "usercopy", "alto", "txt", "amdsec"):
        os.makedirs(os.path.join(target, d))
    written = {}

    def put(rel, data):
        written[rel] = data
        with open(os.path.join(target, rel), "wb") as f:
            f.write(data)
        return rel, len(data), gen.md5_bytes(data)

    src_root = resolve_source_root(spec.get("source_root", ""))
    page_files, page_uuids = {}, {}
    for i, page in enumerate(u["pages"], 1):
        p = f"{i:04d}"
        mc_src = os.path.join(src_root, page["mc"])
        mc_props = jp2_props(mc_src)
        mc_name = f"mc_{psp}_{p}.jp2"
        ac = None
        if page.get("ac"):
            ac_src = os.path.join(src_root, page["ac"])
            # PS objekt (OBJ_001, MIX_001) je v kazdem amd_mets povinny; kdyz archivni sken chybi nebo je prazdny,
            # popise se pod jeho nazvem hodnotami MC
            ac = {"name": os.path.basename(ac_src),
                  "props": jp2_props(ac_src) if os.path.getsize(ac_src) > 0 else mc_props}
        alto_bytes, txt_bytes = tools.ocr(mc_src, mc_name, created, spec["institution"],
                                          os.path.join(src_root, page["alto"]) if page.get("alto") else None,
                                          os.path.join(src_root, page["txt"]) if page.get("txt") else None)
        files = {
            "mc": put(f"mastercopy/{mc_name}", open(mc_src, "rb").read()),
            "uc": put(f"usercopy/uc_{psp}_{p}.jp2", open(mc_src, "rb").read() if no_uc else tools.user_copy(mc_src)),
            "alto": put(f"alto/alto_{psp}_{p}.xml", alto_bytes),
            "txt": put(f"txt/txt_{psp}_{p}.txt", txt_bytes),
        }
        files["amd"] = put(f"amdsec/amd_mets_{psp}_{p}.xml",
                           amd_mets_real(psp, mets_type, label, i, files, mc_props, ac, spec, agents, created).encode("utf-8"))
        page_files[i] = files
        page_uuids[i] = page.get("uuid") or gen_page_uuid(u["uuid"], i)
        print(f"  strana {p}: {page['mc']} {mc_props['width']}x{mc_props['height']}, ALTO {len(alto_bytes)} B, TXT {len(txt_bytes)} B")

    put(f"mets_{psp}.xml", main_mets(psp, spec, u, page_files, page_uuids, created).encode("utf-8"))
    md5_name = f"md5_{psp}.md5"
    md5_lines = "".join(f"{gen.md5_bytes(data)} /{rel}\r\n" for rel, data in sorted(written.items()))
    md5_rel = put(md5_name, md5_lines.encode("utf-8"))
    info_name = f"info_{psp}.xml"
    items = sorted(written.keys()) + [info_name]
    size_kb = max(1, sum(len(d) for d in written.values()) // 1024)
    put(info_name, info_xml(psp, spec, items, size_kb, md5_name, md5_rel[2], created).encode("utf-8"))
    return target


def resolve_source_root(pattern: str) -> str:
    """source_root muze byt glob (nazvy adresaru s nestandardnim kodovanim), musi odpovidat presne jednomu adresari."""
    import glob
    if not pattern or os.path.isdir(pattern):
        return pattern
    hits = [h for h in glob.glob(pattern) if os.path.isdir(h)]
    if len(hits) != 1:
        sys.exit(f"source_root '{pattern}' odpovídá {len(hits)} adresářům, očekáván jeden")
    return hits[0]


def gen_page_uuid(unit_uuid: str, i: int) -> str:
    """Deterministicke uuid strany odvozene z uuid jednotky (uuid5), aby opakovane sestaveni dalo stejne hodnoty."""
    import uuid
    return str(uuid.uuid5(uuid.UUID(unit_uuid), f"page-{i}"))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("spec", help="JSON predpis")
    ap.add_argument("out", help="vystupni adresar; balicek = <out>/<psp>")
    ap.add_argument("--unit", action="append", default=[], help="sestavit jen jednotku s timto psp (lze opakovat)")
    ap.add_argument("--no-uc", action="store_true", help="nevyrabet ztratovou UC (zkopiruje MC); pro rychle testy")
    ap.add_argument("--kakadu-bin", default=KAKADU_BIN_DEFAULT)
    ap.add_argument("--kakadu-lib", default=KAKADU_LIB_DEFAULT)
    ap.add_argument("--lang", default="ces", help="jazyk tesseractu")
    a = ap.parse_args()
    spec = json.load(open(a.spec, encoding="utf-8"))
    tools = Tools(a.kakadu_bin, a.kakadu_lib, a.lang, os.path.join(a.out, ".work"))
    units = [u for u in spec["units"] if not a.unit or u["psp"] in a.unit]
    if not units:
        sys.exit("žádná jednotka k sestavení")
    for u in units:
        print(f"== {u['psp']}: {u['title']} ({u['type']}, {len(u['pages'])} stran)")
        target = build_unit(spec, u, a.out, tools, a.no_uc)
        print("OK:", target)


if __name__ == "__main__":
    main()
