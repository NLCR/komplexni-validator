#!/usr/bin/env python3
"""
Generator synthetickeho SIP balicku podle DMF Jednotky fondu 0.1 (draft, 9. 12. 2025).

Balicek je stavěn primo z tabulek specifikace (kap. 5, 6, 7), ne z hotove fDMF, aby slouzil jako
nezavisla kontrola pravidel. Kapitoly amdSec/fileSec/copyright spec prebira z DMF Periodika 2.1,
proto amd_mets vychazi z perio 2.1/2.2 (kap. 7.5, 7.6, 7.7.2).

Pouziti:
    python3 gen_fund_unit_package.py <cilovy_adresar_balicku> [--variant clipping|clipping_index|card_index]
                                     [--pages N] [--with-directory] [--mutation NAME]...

Obrazove soubory jsou jen placeholdery (validace obrazu se v testech vypina), ale velikosti a MD5
v METS/MD5/INFO odpovidaji skutecnemu obsahu, aby prosly kontroly checksumu.

Mutace (K6): --mutation NAME, seznam pres --list-mutations; kazda je jednoducha zmena vygenerovanych dat
a komentar u ni rika, ktere pravidlo ma chybu odhalit.
"""
import argparse
import hashlib
import os
import sys
from datetime import datetime

CREATED = "2026-09-01T10:00:00"
CREATED_DAY = "2026-09-01"
SIGLA = "ABA001"
COLLECTION_UUID = "5f6a8c2e-1111-4d0e-9a3b-000000000001"
DIRECTORY_UUID = "5f6a8c2e-2222-4d0e-9a3b-000000000002"
UNIT_UUID = "5f6a8c2e-3333-4d0e-9a3b-000000000003"
UNIT_TITLE = "Klementinum a jeho knihovny"
PAGE_UUID_PREFIX = "5f6a8c2e-4444-4d0e-9a3b-0000000000"  # + 2 cislice

VARIANTS = {
    # varianta: (METS @TYPE, mods:genre, LABEL, extent)
    "clipping": ("Clipping", "clipping", "Výstřižek: Klementinum a jeho knihovny", "4 části článku"),
    "clipping_index": ("Clipping index", "clippingIndex", "Soubor výstřižků: Klementinum", "15 článků (20 vystřižených částí)"),
    "card_index": ("Card index", "cardIndex", "Soubor kartotéčních lístků: Klementinum", "32 kartotéčních lístků"),
}

METS_NS = ('xmlns:mets="http://www.loc.gov/METS/" xmlns:mods="http://www.loc.gov/mods/v3" '
           'xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" '
           'xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" '
           'xmlns:mix="http://www.loc.gov/mix/v20" xmlns:premis="info:lc/xmlns/premis-v2"')
SCHEMA_LOCATION = ('http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/mets.xsd '
                   'http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-8.xsd '
                   'http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/simpledc20021212.xsd '
                   'http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd '
                   'http://www.loc.gov/mix/v20 http://www.loc.gov/standards/mix/mix20/mix20.xsd '
                   'info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis-v2-2.xsd')


def md5_bytes(data: bytes) -> str:
    return hashlib.md5(data).hexdigest()


def page_uuid(i: int) -> str:
    return PAGE_UUID_PREFIX + "%02d" % i


# ----------------------------------------------------------------------------------------------------
# soubory stran

def jp2_placeholder(seed: str) -> bytes:
    """Minimalni JP2 signature box + smeti; skutecny obraz se v testech nevaliduje."""
    sig = bytes.fromhex("0000000C6A5020200D0A870A")
    body = hashlib.sha256(seed.encode()).digest() * 8
    return sig + body


ALTO_VERSIONS = {
    # verze -> (namespace, schemaLocation); predpis OCR (ALTO XML a TXT OCR) 1.0 pripousti ALTO 2.0 a novejsi
    "2.0": ("http://www.loc.gov/standards/alto/ns-v2#", "http://www.loc.gov/standards/alto/alto-v2.0.xsd"),
    "3.0": ("http://www.loc.gov/standards/alto/ns-v3#", "http://www.loc.gov/alto/v3/alto-3-0.xsd"),
    "4.4": ("http://www.loc.gov/standards/alto/ns-v4#", "http://www.loc.gov/standards/alto/v4/alto-4-4.xsd"),
    # neexistujici verze pro mutaci alto-unknown-version
    "9.9": ("http://www.loc.gov/standards/alto/ns-v9#", "http://www.loc.gov/standards/alto/v9/alto-9-9.xsd"),
}


def alto_xml(page_no: int, version: str = "4.4") -> str:
    ns, loc = ALTO_VERSIONS[version]
    return f'''<?xml version="1.0" encoding="UTF-8"?>
<alto xmlns="{ns}" xmlns:xlink="http://www.w3.org/1999/xlink"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="{ns} {loc}">
    <Description>
        <MeasurementUnit>pixel</MeasurementUnit>
        <sourceImageInformation>
            <fileName>mc_{{psp}}_{page_no:04d}.jp2</fileName>
        </sourceImageInformation>
        <OCRProcessing ID="OCR_1">
            <ocrProcessingStep>
                <processingDateTime>{CREATED}</processingDateTime>
                <processingSoftware>
                    <softwareCreator>ABBYY</softwareCreator>
                    <softwareName>ABBYY FineReader Server</softwareName>
                    <softwareVersion>14.0</softwareVersion>
                </processingSoftware>
            </ocrProcessingStep>
        </OCRProcessing>
    </Description>
    <Layout>
        <Page ID="PAGE_{page_no:04d}" PHYSICAL_IMG_NR="{page_no}" HEIGHT="2806" WIDTH="1685">
            <PrintSpace HEIGHT="2600" WIDTH="1500" HPOS="90" VPOS="100">
                <TextBlock ID="BLOCK_{page_no:04d}_1" HEIGHT="60" WIDTH="800" HPOS="100" VPOS="120">
                    <TextLine ID="LINE_{page_no:04d}_1" HEIGHT="40" WIDTH="800" HPOS="100" VPOS="130">
                        <String ID="STR_{page_no:04d}_1" CONTENT="Klementinum" HEIGHT="40" WIDTH="380" HPOS="100" VPOS="130"/>
                        <SP WIDTH="20" HPOS="480" VPOS="130"/>
                        <String ID="STR_{page_no:04d}_2" CONTENT="strana" HEIGHT="40" WIDTH="200" HPOS="500" VPOS="130"/>
                        <SP WIDTH="20" HPOS="700" VPOS="130"/>
                        <String ID="STR_{page_no:04d}_3" CONTENT="{page_no}" HEIGHT="40" WIDTH="40" HPOS="720" VPOS="130"/>
                    </TextLine>
                </TextBlock>
            </PrintSpace>
        </Page>
    </Layout>
</alto>
'''


def txt_content(page_no: int) -> str:
    return f"Klementinum strana {page_no}\nVýstřižek z novin, testovací OCR text s diakritikou: příliš žluťoučký kůň.\n"


# ----------------------------------------------------------------------------------------------------
# amd_mets (DMF Periodika 2.1/2.2, kap. 7.5, 7.6.2, 7.7.2)

def premis_object(obj_id: str, ident: str, level: str, size: int, digest: str, fmt_name: str, fmt_ver: str,
                  pronom: str, app: str, app_ver: str, orig_name: str, relationship: str = "", linking_events=()):
    links = "".join(f'''
                        <premis:linkingEventIdentifier>
                            <premis:linkingEventIdentifierType>NK_eventID</premis:linkingEventIdentifierType>
                            <premis:linkingEventIdentifierValue>{e}</premis:linkingEventIdentifierValue>
                        </premis:linkingEventIdentifier>''' for e in linking_events)
    return f'''
            <mets:techMD ID="{obj_id}">
                <mets:mdWrap MDTYPE="PREMIS" MIMETYPE="text/xml">
                    <mets:xmlData>
                        <premis:object xsi:type="premis:file">
                            <premis:objectIdentifier>
                                <premis:objectIdentifierType>file</premis:objectIdentifierType>
                                <premis:objectIdentifierValue>{ident}</premis:objectIdentifierValue>
                            </premis:objectIdentifier>
                            <premis:preservationLevel>
                                <premis:preservationLevelValue>{level}</premis:preservationLevelValue>
                                <premis:preservationLevelDateAssigned>{CREATED_DAY}</premis:preservationLevelDateAssigned>
                            </premis:preservationLevel>
                            <premis:objectCharacteristics>
                                <premis:compositionLevel>0</premis:compositionLevel>
                                <premis:fixity>
                                    <premis:messageDigestAlgorithm>MD5</premis:messageDigestAlgorithm>
                                    <premis:messageDigest>{digest}</premis:messageDigest>
                                    <premis:messageDigestOriginator>gen_fund_unit_package</premis:messageDigestOriginator>
                                </premis:fixity>
                                <premis:size>{size}</premis:size>
                                <premis:format>
                                    <premis:formatDesignation>
                                        <premis:formatName>{fmt_name}</premis:formatName>
                                        <premis:formatVersion>{fmt_ver}</premis:formatVersion>
                                    </premis:formatDesignation>
                                    <premis:formatRegistry>
                                        <premis:formatRegistryName>PRONOM</premis:formatRegistryName>
                                        <premis:formatRegistryKey>{pronom}</premis:formatRegistryKey>
                                    </premis:formatRegistry>
                                </premis:format>
                                <premis:creatingApplication>
                                    <premis:creatingApplicationName>{app}</premis:creatingApplicationName>
                                    <premis:creatingApplicationVersion>{app_ver}</premis:creatingApplicationVersion>
                                    <premis:dateCreatedByApplication>{CREATED}</premis:dateCreatedByApplication>
                                </premis:creatingApplication>
                            </premis:objectCharacteristics>
                            <premis:originalName>{orig_name}</premis:originalName>{relationship}{links}
                        </premis:object>
                    </mets:xmlData>
                </mets:mdWrap>
            </mets:techMD>'''


def premis_relationship(related_obj: str, related_event: str) -> str:
    return f'''
                            <premis:relationship>
                                <premis:relationshipType>derivation</premis:relationshipType>
                                <premis:relationshipSubType>created from</premis:relationshipSubType>
                                <premis:relatedObjectIdentification>
                                    <premis:relatedObjectIdentifierType>file</premis:relatedObjectIdentifierType>
                                    <premis:relatedObjectIdentifierValue>{related_obj}</premis:relatedObjectIdentifierValue>
                                </premis:relatedObjectIdentification>
                                <premis:relatedEventIdentification>
                                    <premis:relatedEventIdentifierType>NK_eventID</premis:relatedEventIdentifierType>
                                    <premis:relatedEventIdentifierValue>{related_event}</premis:relatedEventIdentifierValue>
                                </premis:relatedEventIdentification>
                            </premis:relationship>'''


def premis_event(evt_id: str, ident: str, etype: str, detail: str, agent: str, agent_role: str, obj: str) -> str:
    return f'''
            <mets:digiprovMD ID="{evt_id}">
                <mets:mdWrap MDTYPE="PREMIS" MIMETYPE="text/xml">
                    <mets:xmlData>
                        <premis:event>
                            <premis:eventIdentifier>
                                <premis:eventIdentifierType>NK_eventID</premis:eventIdentifierType>
                                <premis:eventIdentifierValue>{ident}</premis:eventIdentifierValue>
                            </premis:eventIdentifier>
                            <premis:eventType>{etype}</premis:eventType>
                            <premis:eventDateTime>{CREATED}</premis:eventDateTime>
                            <premis:eventDetail>{detail}</premis:eventDetail>
                            <premis:eventOutcomeInformation>
                                <premis:eventOutcome>OK</premis:eventOutcome>
                            </premis:eventOutcomeInformation>
                            <premis:linkingAgentIdentifier>
                                <premis:linkingAgentIdentifierType>NK_AgentID</premis:linkingAgentIdentifierType>
                                <premis:linkingAgentIdentifierValue>{agent}</premis:linkingAgentIdentifierValue>
                                <premis:linkingAgentRole>{agent_role}</premis:linkingAgentRole>
                            </premis:linkingAgentIdentifier>
                            <premis:linkingObjectIdentifier>
                                <premis:linkingObjectIdentifierType>file</premis:linkingObjectIdentifierType>
                                <premis:linkingObjectIdentifierValue>{obj}</premis:linkingObjectIdentifierValue>
                            </premis:linkingObjectIdentifier>
                        </premis:event>
                    </mets:xmlData>
                </mets:mdWrap>
            </mets:digiprovMD>'''


def premis_agent(agent_id: str, ident: str, name: str, atype: str) -> str:
    return f'''
            <mets:digiprovMD ID="{agent_id}">
                <mets:mdWrap MDTYPE="PREMIS" MIMETYPE="text/xml">
                    <mets:xmlData>
                        <premis:agent>
                            <premis:agentIdentifier>
                                <premis:agentIdentifierType>NK_AgentID</premis:agentIdentifierType>
                                <premis:agentIdentifierValue>{ident}</premis:agentIdentifierValue>
                            </premis:agentIdentifier>
                            <premis:agentName>{name}</premis:agentName>
                            <premis:agentType>{atype}</premis:agentType>
                        </premis:agent>
                    </mets:xmlData>
                </mets:mdWrap>
            </mets:digiprovMD>'''


def mix_ps(ident: str) -> str:
    return f'''
            <mets:techMD ID="MIX_001">
                <mets:mdWrap MDTYPE="NISOIMG" MIMETYPE="text/xml">
                    <mets:xmlData>
                        <mix:mix>
                            <mix:BasicDigitalObjectInformation>
                                <mix:ObjectIdentifier>
                                    <mix:objectIdentifierType>JHOVE</mix:objectIdentifierType>
                                    <mix:objectIdentifierValue>{ident}</mix:objectIdentifierValue>
                                </mix:ObjectIdentifier>
                                <mix:FormatDesignation>
                                    <mix:formatName>image/tiff</mix:formatName>
                                    <mix:formatVersion>6.0</mix:formatVersion>
                                </mix:FormatDesignation>
                                <mix:byteOrder>little endian</mix:byteOrder>
                                <mix:Compression>
                                    <mix:compressionScheme>Uncompressed</mix:compressionScheme>
                                </mix:Compression>
                            </mix:BasicDigitalObjectInformation>
                            <mix:BasicImageInformation>
                                <mix:BasicImageCharacteristics>
                                    <mix:imageWidth>1685</mix:imageWidth>
                                    <mix:imageHeight>2806</mix:imageHeight>
                                    <mix:PhotometricInterpretation>
                                        <mix:colorSpace>RGB</mix:colorSpace>
                                    </mix:PhotometricInterpretation>
                                </mix:BasicImageCharacteristics>
                            </mix:BasicImageInformation>
                            <mix:ImageCaptureMetadata>
                                <mix:GeneralCaptureInformation>
                                    <mix:dateTimeCreated>{CREATED}</mix:dateTimeCreated>
                                    <mix:imageProducer>Národní knihovna ČR</mix:imageProducer>
                                    <mix:captureDevice>reflection print scanner</mix:captureDevice>
                                </mix:GeneralCaptureInformation>
                                <mix:ScannerCapture>
                                    <mix:scannerManufacturer>Zeutschel</mix:scannerManufacturer>
                                    <mix:ScannerModel>
                                        <mix:scannerModelName>OS 15000</mix:scannerModelName>
                                        <mix:scannerModelNumber>OS 15000</mix:scannerModelNumber>
                                        <mix:scannerModelSerialNo>TEST-0001</mix:scannerModelSerialNo>
                                    </mix:ScannerModel>
                                    <mix:MaximumOpticalResolution>
                                        <mix:xOpticalResolution>600</mix:xOpticalResolution>
                                        <mix:yOpticalResolution>600</mix:yOpticalResolution>
                                        <mix:opticalResolutionUnit>in.</mix:opticalResolutionUnit>
                                    </mix:MaximumOpticalResolution>
                                    <mix:scannerSensor>ColorTriLinear</mix:scannerSensor>
                                    <mix:ScanningSystemSoftware>
                                        <mix:scanningSoftwareName>Omniscan</mix:scanningSoftwareName>
                                        <mix:scanningSoftwareVersionNo>12.0</mix:scanningSoftwareVersionNo>
                                    </mix:ScanningSystemSoftware>
                                </mix:ScannerCapture>
                                <mix:orientation>normal*</mix:orientation>
                            </mix:ImageCaptureMetadata>
                            <mix:ImageAssessmentMetadata>
                                <mix:SpatialMetrics>
                                    <mix:samplingFrequencyUnit>in.</mix:samplingFrequencyUnit>
                                    <mix:xSamplingFrequency>
                                        <mix:numerator>300</mix:numerator>
                                        <mix:denominator>1</mix:denominator>
                                    </mix:xSamplingFrequency>
                                    <mix:ySamplingFrequency>
                                        <mix:numerator>300</mix:numerator>
                                        <mix:denominator>1</mix:denominator>
                                    </mix:ySamplingFrequency>
                                </mix:SpatialMetrics>
                                <mix:ImageColorEncoding>
                                    <mix:BitsPerSample>
                                        <mix:bitsPerSampleValue>8</mix:bitsPerSampleValue>
                                        <mix:bitsPerSampleValue>8</mix:bitsPerSampleValue>
                                        <mix:bitsPerSampleValue>8</mix:bitsPerSampleValue>
                                        <mix:bitsPerSampleUnit>integer</mix:bitsPerSampleUnit>
                                    </mix:BitsPerSample>
                                    <mix:samplesPerPixel>3</mix:samplesPerPixel>
                                </mix:ImageColorEncoding>
                            </mix:ImageAssessmentMetadata>
                        </mix:mix>
                    </mets:xmlData>
                </mets:mdWrap>
            </mets:techMD>'''


def mix_mc(ident: str, source: str) -> str:
    return f'''
            <mets:techMD ID="MIX_002">
                <mets:mdWrap MDTYPE="NISOIMG" MIMETYPE="text/xml">
                    <mets:xmlData>
                        <mix:mix>
                            <mix:BasicDigitalObjectInformation>
                                <mix:ObjectIdentifier>
                                    <mix:objectIdentifierType>JHOVE</mix:objectIdentifierType>
                                    <mix:objectIdentifierValue>{ident}</mix:objectIdentifierValue>
                                </mix:ObjectIdentifier>
                                <mix:FormatDesignation>
                                    <mix:formatName>image/jp2</mix:formatName>
                                    <mix:formatVersion>1.0</mix:formatVersion>
                                </mix:FormatDesignation>
                                <mix:byteOrder>big endian</mix:byteOrder>
                                <mix:Compression>
                                    <mix:compressionScheme>JPEG 2000 Lossless</mix:compressionScheme>
                                </mix:Compression>
                            </mix:BasicDigitalObjectInformation>
                            <mix:BasicImageInformation>
                                <mix:BasicImageCharacteristics>
                                    <mix:imageWidth>1685</mix:imageWidth>
                                    <mix:imageHeight>2806</mix:imageHeight>
                                    <mix:PhotometricInterpretation>
                                        <mix:colorSpace>RGB</mix:colorSpace>
                                    </mix:PhotometricInterpretation>
                                </mix:BasicImageCharacteristics>
                                <mix:SpecialFormatCharacteristics>
                                    <mix:JPEG2000>
                                        <mix:CodecCompliance>
                                            <mix:codec>Kakadu</mix:codec>
                                            <mix:codecVersion>8.0</mix:codecVersion>
                                            <mix:codestreamProfile>P1</mix:codestreamProfile>
                                            <mix:complianceClass>C1</mix:complianceClass>
                                        </mix:CodecCompliance>
                                        <mix:EncodingOptions>
                                            <mix:Tiles>
                                                <mix:tileWidth>4096</mix:tileWidth>
                                                <mix:tileHeight>4096</mix:tileHeight>
                                            </mix:Tiles>
                                            <mix:qualityLayers>1</mix:qualityLayers>
                                            <mix:resolutionLevels>5</mix:resolutionLevels>
                                        </mix:EncodingOptions>
                                    </mix:JPEG2000>
                                </mix:SpecialFormatCharacteristics>
                            </mix:BasicImageInformation>
                            <mix:ImageAssessmentMetadata>
                                <mix:SpatialMetrics>
                                    <mix:samplingFrequencyUnit>in.</mix:samplingFrequencyUnit>
                                    <mix:xSamplingFrequency>
                                        <mix:numerator>300</mix:numerator>
                                        <mix:denominator>1</mix:denominator>
                                    </mix:xSamplingFrequency>
                                    <mix:ySamplingFrequency>
                                        <mix:numerator>300</mix:numerator>
                                        <mix:denominator>1</mix:denominator>
                                    </mix:ySamplingFrequency>
                                </mix:SpatialMetrics>
                                <mix:ImageColorEncoding>
                                    <mix:BitsPerSample>
                                        <mix:bitsPerSampleValue>8</mix:bitsPerSampleValue>
                                        <mix:bitsPerSampleValue>8</mix:bitsPerSampleValue>
                                        <mix:bitsPerSampleValue>8</mix:bitsPerSampleValue>
                                        <mix:bitsPerSampleUnit>integer</mix:bitsPerSampleUnit>
                                    </mix:BitsPerSample>
                                    <mix:samplesPerPixel>3</mix:samplesPerPixel>
                                </mix:ImageColorEncoding>
                            </mix:ImageAssessmentMetadata>
                            <mix:ChangeHistory>
                                <mix:ImageProcessing>
                                    <mix:dateTimeProcessed>{CREATED}</mix:dateTimeProcessed>
                                    <mix:sourceData>{source}</mix:sourceData>
                                </mix:ImageProcessing>
                            </mix:ChangeHistory>
                        </mix:mix>
                    </mets:xmlData>
                </mets:mdWrap>
            </mets:techMD>'''


def amd_mets_xml(psp: str, mets_type: str, label: str, page_no: int, files: dict) -> str:
    """files: {'mc': (relpath, size, md5), 'uc': ..., 'alto': ..., 'txt': ...}"""
    p = f"{page_no:04d}"
    ps_id, mc_id, alto_id = f"PS_{psp}_{p}", f"MC_{psp}_{p}", f"XML_{psp}_{p}"
    ev_capture, ev_delete, ev_mc, ev_alto = f"capture_{p}", f"deletion_{p}", f"mc_creation_{p}", f"alto_creation_{p}"
    ag_scan, ag_pp, ag_kdu, ag_ocr = "Zeutschel-OS15000", "TM-postprocessing", "Kakadu-8.0", "ABBYY-14"
    mc, uc, alto, txt = files["mc"], files["uc"], files["alto"], files["txt"]
    objects = (
        premis_object("OBJ_001", ps_id, "deleted", 14186290, md5_bytes(ps_id.encode()), "image/tiff", "6.0",
                      "fmt/353", "Omniscan", "12.0", f"{p}.tif", linking_events=(ev_capture, ev_delete))
        + premis_object("OBJ_002", mc_id, "preservation", mc[1], mc[2], "image/jp2", "1.0", "x-fmt/392",
                        "Kakadu kdu_compress", "8.0", os.path.basename(mc[0]),
                        relationship=premis_relationship(ps_id, ev_mc), linking_events=(ev_mc,))
        + premis_object("OBJ_003", alto_id, "preservation", alto[1], alto[2], "text/xml", "1.0", "fmt/101",
                        "ABBYY FineReader Server", "14.0", os.path.basename(alto[0]),
                        relationship=premis_relationship(mc_id, ev_alto))
    )
    mixes = mix_ps(ps_id) + mix_mc(mc_id, f"{p}.tif")
    events = (
        premis_event("EVT_001", ev_capture, "capture", "capture/digitization", ag_scan, "machine", ps_id)
        + premis_event("EVT_002", ev_delete, "deletion", "deletion/PS_deletion", ag_pp, "software", ps_id)
        + premis_event("EVT_003", ev_mc, "migration", "migration/MC_creation", ag_kdu, "software", mc_id)
        + premis_event("EVT_004", ev_alto, "capture", "capture/XML_creation", ag_ocr, "software", alto_id)
    )
    agents = (
        premis_agent("AGENT_001", ag_scan, "Zeutschel OS 15000", "hardware")
        + premis_agent("AGENT_002", ag_pp, "TM 1.4", "software")
        + premis_agent("AGENT_003", ag_kdu, "Kakadu 8.0", "software")
        + premis_agent("AGENT_004", ag_ocr, "ABBYY FineReader Server 14", "software")
    )

    def file_el(fid, rel, size, digest, mime, admid=""):
        adm = f' ADMID="{admid}"' if admid else ""
        return f'''
            <mets:file ID="{fid}" MIMETYPE="{mime}" SIZE="{size}" CHECKSUMTYPE="MD5" CHECKSUM="{digest}" SEQ="{page_no - 1}" CREATED="{CREATED}"{adm}>
                <mets:FLocat LOCTYPE="URL" xlink:href="{rel}"/>
            </mets:file>'''

    return f'''<?xml version="1.0" encoding="UTF-8"?>
<mets:mets {METS_NS} LABEL="{label}" TYPE="{mets_type}" xsi:schemaLocation="{SCHEMA_LOCATION}">
    <mets:metsHdr CREATEDATE="{CREATED}" LASTMODDATE="{CREATED}">
        <mets:agent ROLE="CREATOR" TYPE="ORGANIZATION">
            <mets:name>{SIGLA}</mets:name>
        </mets:agent>
        <mets:agent ROLE="ARCHIVIST" TYPE="ORGANIZATION">
            <mets:name>{SIGLA}</mets:name>
        </mets:agent>
    </mets:metsHdr>
    <mets:amdSec ID="PAGE{p}">{objects}{mixes}{events}{agents}
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
        <mets:div TYPE="UNIT" ID="DIV_UNIT_{p}" LABEL="{label}">
            <mets:fptr FILEID="MC_{p}"/>
            <mets:fptr FILEID="UC_{p}"/>
            <mets:fptr FILEID="ALTO_{p}"/>
            <mets:fptr FILEID="TXT_{p}"/>
        </mets:div>
    </mets:structMap>
</mets:mets>
'''


# ----------------------------------------------------------------------------------------------------
# bibliograficka metadata (spec kap. 7.3.1 az 7.3.4)

def mods_record_info(standard: str) -> str:
    return f'''
                        <mods:recordInfo>
                            <mods:descriptionStandard>{standard}</mods:descriptionStandard>
                            <mods:recordContentSource authority="siglaADR">{SIGLA}</mods:recordContentSource>
                            <mods:recordCreationDate encoding="iso8601">{CREATED}</mods:recordCreationDate>
                            <mods:recordChangeDate encoding="iso8601">{CREATED}</mods:recordChangeDate>
                            <mods:recordOrigin>human prepared</mods:recordOrigin>
                            <mods:languageOfCataloging>
                                <mods:languageTerm authority="iso639-2b" type="code">cze</mods:languageTerm>
                            </mods:languageOfCataloging>
                        </mods:recordInfo>'''


def mods_location(shelf: str) -> str:
    return f'''
                        <mods:location>
                            <mods:physicalLocation authority="siglaADR">{SIGLA}</mods:physicalLocation>
                            <mods:shelfLocator>{shelf}</mods:shelfLocator>
                        </mods:location>'''


def dmdsec_mods(sec_id: str, body: str) -> str:
    return f'''
    <mets:dmdSec ID="{sec_id}">
        <mets:mdWrap MDTYPE="MODS" MDTYPEVERSION="3.8" MIMETYPE="text/xml">
            <mets:xmlData>{body}
            </mets:xmlData>
        </mets:mdWrap>
    </mets:dmdSec>'''


def dmdsec_dc(sec_id: str, body: str) -> str:
    return f'''
    <mets:dmdSec ID="{sec_id}">
        <mets:mdWrap MDTYPE="DC" MIMETYPE="text/xml">
            <mets:xmlData>
                <oai_dc:dc xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">{body}
                </oai_dc:dc>
            </mets:xmlData>
        </mets:mdWrap>
    </mets:dmdSec>'''


def mods_unitcollection(standard: str) -> str:
    return dmdsec_mods("MODSMD_UNITCOLLECTION_0001", f'''
                    <mods:mods ID="MODS_UNITCOLLECTION_0001" version="3.8">
                        <mods:titleInfo>
                            <mods:title>Výstřižky ke Klementinu</mods:title>
                            <mods:subTitle>sbírka novinových výstřižků o Klementinu a jeho knihovnách</mods:subTitle>
                        </mods:titleInfo>
                        <mods:name type="corporate">
                            <mods:namePart>Národní knihovna České republiky</mods:namePart>
                            <mods:role>
                                <mods:roleTerm type="code" authority="marcrelator">col</mods:roleTerm>
                            </mods:role>
                        </mods:name>
                        <mods:genre>unit collection</mods:genre>
                        <mods:originInfo>
                            <mods:dateCreated point="start">1920</mods:dateCreated>
                            <mods:dateCreated point="end" qualifier="approximate">1965</mods:dateCreated>
                        </mods:originInfo>
                        <mods:physicalDescription>
                            <mods:extent>12 složek</mods:extent>
                            <mods:note>část výstřižků poškozena vlhkostí</mods:note>
                        </mods:physicalDescription>
                        <mods:note>Sbírka výstřižků shromážděná v knihovně.</mods:note>
                        <mods:subject authority="czenas">
                            <mods:topic>dějiny knihoven</mods:topic>
                            <mods:geographic>Klementinum (Praha, Česko)</mods:geographic>
                        </mods:subject>
                        <mods:identifier type="uuid">{COLLECTION_UUID}</mods:identifier>
                        <mods:identifier type="sysno">000123456</mods:identifier>{mods_location("54 A 001")}{mods_record_info(standard)}
                    </mods:mods>''')


def dc_unitcollection() -> str:
    return dmdsec_dc("DCMD_UNITCOLLECTION_0001", f'''
                    <dc:title>Výstřižky ke Klementinu : sbírka novinových výstřižků o Klementinu a jeho knihovnách</dc:title>
                    <dc:creator>Národní knihovna České republiky</dc:creator>
                    <dc:type>model:unitcollection</dc:type>
                    <dc:date>1920-1965</dc:date>
                    <dc:format>12 složek</dc:format>
                    <dc:description>Sbírka výstřižků shromážděná v knihovně.</dc:description>
                    <dc:subject>dějiny knihoven</dc:subject>
                    <dc:subject>Klementinum (Praha, Česko)</dc:subject>
                    <dc:identifier>uuid:{COLLECTION_UUID}</dc:identifier>
                    <dc:identifier>sysno:000123456</dc:identifier>
                    <dc:source>{SIGLA}</dc:source>
                    <dc:source>54 A 001</dc:source>''')


def mods_directory(standard: str) -> str:
    return dmdsec_mods("MODSMD_DIRECTORY_0001", f'''
                    <mods:mods ID="MODS_DIRECTORY_0001" version="3.8">
                        <mods:titleInfo>
                            <mods:title>Klementinum 1950-1959</mods:title>
                            <mods:partNumber>3</mods:partNumber>
                        </mods:titleInfo>
                        <mods:genre>directory</mods:genre>
                        <mods:originInfo>
                            <mods:dateCreated point="start">1950</mods:dateCreated>
                            <mods:dateCreated point="end">1959</mods:dateCreated>
                        </mods:originInfo>
                        <mods:physicalDescription>
                            <mods:extent>40 výstřižků</mods:extent>
                        </mods:physicalDescription>
                        <mods:identifier type="uuid">{DIRECTORY_UUID}</mods:identifier>
                        <mods:identifier type="accession">1959/0003</mods:identifier>{mods_location("54 A 001/3")}{mods_record_info(standard)}
                    </mods:mods>''')


def dc_directory() -> str:
    return dmdsec_dc("DCMD_DIRECTORY_0001", f'''
                    <dc:title>Klementinum 1950-1959. 3</dc:title>
                    <dc:type>model:directory</dc:type>
                    <dc:date>1950-1959</dc:date>
                    <dc:format>40 výstřižků</dc:format>
                    <dc:identifier>uuid:{DIRECTORY_UUID}</dc:identifier>
                    <dc:identifier>accession:1959/0003</dc:identifier>
                    <dc:source>{SIGLA}</dc:source>
                    <dc:source>54 A 001/3</dc:source>''')


def mods_unit(standard: str, genre: str, extent: str, urnnbn: str) -> str:
    if standard == "rda":
        origin = '''
                            <mods:originInfo eventType="publication">
                                <mods:place>
                                    <mods:placeTerm type="text">Praha</mods:placeTerm>
                                    <mods:placeTerm type="code" authority="marccountry">xr</mods:placeTerm>
                                </mods:place>
                                <mods:agent>
                                    <mods:namePart>Melantrich</mods:namePart>
                                    <mods:role>
                                        <mods:roleTerm type="text">publisher</mods:roleTerm>
                                    </mods:role>
                                </mods:agent>
                                <mods:dateIssued>1932</mods:dateIssued>
                            </mods:originInfo>'''
    else:
        origin = '''
                            <mods:originInfo>
                                <mods:place>
                                    <mods:placeTerm type="text">Praha</mods:placeTerm>
                                    <mods:placeTerm type="code" authority="marccountry">xr</mods:placeTerm>
                                </mods:place>
                                <mods:agent>
                                    <mods:namePart>Melantrich</mods:namePart>
                                    <mods:role>
                                        <mods:roleTerm type="text">publisher</mods:roleTerm>
                                    </mods:role>
                                </mods:agent>
                                <mods:dateIssued>1932</mods:dateIssued>
                            </mods:originInfo>'''
    return dmdsec_mods("MODSMD_UNIT_0001", f'''
                    <mods:mods ID="MODS_UNIT_0001" version="3.8">
                        <mods:titleInfo>
                            <mods:title>{UNIT_TITLE}</mods:title>
                            <mods:subTitle>k výročí Národní a universitní knihovny</mods:subTitle>
                        </mods:titleInfo>
                        <mods:name type="personal" usage="primary">
                            <mods:namePart type="family">Novák</mods:namePart>
                            <mods:namePart type="given">Jan</mods:namePart>
                            <mods:nameIdentifier>jk01090123</mods:nameIdentifier>
                            <mods:role>
                                <mods:roleTerm type="code" authority="marcrelator">aut</mods:roleTerm>
                            </mods:role>
                        </mods:name>
                        <mods:genre>{genre}</mods:genre>
                        <mods:genre>článek</mods:genre>
                        <mods:language>
                            <mods:languageTerm type="code" authority="iso639-2b">cze</mods:languageTerm>
                        </mods:language>
                        <mods:physicalDescription>
                            <mods:form authority="marcform">print</mods:form>
                            <mods:extent>{extent}</mods:extent>
                            <mods:note>výstřižek nalepen na kartonu</mods:note>
                        </mods:physicalDescription>
                        <mods:note>Výstřižek z deníku České slovo.</mods:note>
                        <mods:subject authority="czenas">
                            <mods:topic>knihovny</mods:topic>
                            <mods:geographic>Praha (Česko)</mods:geographic>
                            <mods:temporal>20. století</mods:temporal>
                        </mods:subject>
                        <mods:relatedItem type="host">
                            <mods:titleInfo>
                                <mods:title>České slovo</mods:title>
                            </mods:titleInfo>{origin}
                            <mods:note type="kramerius-uuid" xlink:href="uuid:1a2b3c4d-0000-4000-8000-000000000099">Kramerius-digitalizovaná verze periodika</mods:note>
                            <mods:identifier type="issn">1802-7253</mods:identifier>
                            <mods:part>
                                <mods:detail type="volume">
                                    <mods:number>24</mods:number>
                                </mods:detail>
                                <mods:detail type="issue">
                                    <mods:number>112</mods:number>
                                </mods:detail>
                            </mods:part>
                        </mods:relatedItem>
                        <mods:identifier type="uuid">{UNIT_UUID}</mods:identifier>
                        <mods:identifier type="urnnbn">{urnnbn}</mods:identifier>
                        <mods:identifier type="barcode">2610123456</mods:identifier>{mods_location("54 A 001/3/12")}{mods_record_info(standard)}
                    </mods:mods>''')


def dc_unit(extent: str, urnnbn: str) -> str:
    return dmdsec_dc("DCMD_UNIT_0001", f'''
                    <dc:title>{UNIT_TITLE} : k výročí Národní a universitní knihovny</dc:title>
                    <dc:creator>Novák, Jan</dc:creator>
                    <dc:type>model:unit</dc:type>
                    <dc:type>článek</dc:type>
                    <dc:language>cze</dc:language>
                    <dc:format>print</dc:format>
                    <dc:format>{extent}</dc:format>
                    <dc:description>Výstřižek z deníku České slovo.</dc:description>
                    <dc:subject>knihovny</dc:subject>
                    <dc:subject>Praha (Česko)</dc:subject>
                    <dc:subject>20. století</dc:subject>
                    <dc:identifier>uuid:{UNIT_UUID}</dc:identifier>
                    <dc:identifier>urnnbn:{urnnbn}</dc:identifier>
                    <dc:identifier>barcode:2610123456</dc:identifier>
                    <dc:source>{SIGLA}</dc:source>
                    <dc:source>54 A 001/3/12</dc:source>''')


def mods_page(i: int) -> str:
    return dmdsec_mods(f"MODSMD_PAGE_{i:04d}", f'''
                    <mods:mods ID="MODS_PAGE_{i:04d}" version="3.8">
                        <mods:identifier type="uuid">{page_uuid(i)}</mods:identifier>
                        <mods:part>
                            <mods:detail type="pageNumber">
                                <mods:number>{i}</mods:number>
                            </mods:detail>
                        </mods:part>
                        <mods:part>
                            <mods:detail type="pageIndex">
                                <mods:number>{i}</mods:number>
                            </mods:detail>
                        </mods:part>
                        <mods:typeOfResource>text</mods:typeOfResource>
                    </mods:mods>''')


def dc_page(i: int) -> str:
    return dmdsec_dc(f"DCMD_PAGE_{i:04d}", f'''
                    <dc:identifier>uuid:{page_uuid(i)}</dc:identifier>
                    <dc:type>text</dc:type>''')


# ----------------------------------------------------------------------------------------------------
# hlavni METS (spec kap. 5.7, 7.1, 7.2, 7.6, 7.7; fileSec podle perio 2.2 kap. 7.6.1)

def main_mets_xml(psp: str, mets_type: str, label: str, genre: str, extent: str, urnnbn: str, standard: str,
                  pages: int, with_directory: bool, page_files: dict) -> str:
    dmd = mods_unitcollection(standard) + dc_unitcollection()
    if with_directory:
        dmd += mods_directory(standard) + dc_directory()
    dmd += mods_unit(standard, genre, extent, urnnbn) + dc_unit(extent, urnnbn)
    for i in range(1, pages + 1):
        dmd += mods_page(i) + dc_page(i)

    def grp(gid, use, key, mime, admid=None):
        out = f'\n        <mets:fileGrp ID="{gid}" USE="{use}">'
        for i in range(1, pages + 1):
            rel, size, digest = page_files[i][key]
            fid = f"{key.upper()}_{i:04d}" if key != "amd" else f"AMD_METS_{i:04d}"
            out += f'''
            <mets:file ID="{fid}" MIMETYPE="{mime}" SIZE="{size}" CHECKSUMTYPE="MD5" CHECKSUM="{digest}" SEQ="{i - 1}" CREATED="{CREATED}">
                <mets:FLocat LOCTYPE="URL" xlink:href="{rel}"/>
            </mets:file>'''
        return out + "\n        </mets:fileGrp>"

    filesec = (grp("MC_IMGGRP", "Images", "mc", "image/jp2") + grp("UC_IMGGRP", "Images", "uc", "image/jp2")
               + grp("ALTOGRP", "Layout", "alto", "text/xml") + grp("TXTGRP", "Text", "txt", "text/plain")
               + grp("TECHMDGRP", "Technical Metadata", "amd", "text/xml"))

    unit_div = f'''
                <mets:div TYPE="UNIT" ID="UNIT_0001" LABEL="{label}" DMDID="MODSMD_UNIT_0001">
                    <mets:div TYPE="TITLE" ID="TITLE_0001"/>
                    <mets:div TYPE="NORMAL_TEXT" ID="NORMAL_TEXT_0001"/>
                </mets:div>'''
    if with_directory:
        inner = f'''
            <mets:div TYPE="DIRECTORY" ID="DIRECTORY_0001" LABEL="Klementinum 1950-1959" DMDID="MODSMD_DIRECTORY_0001">{unit_div}
            </mets:div>'''
    else:
        inner = unit_div
    logical = f'''
    <mets:structMap TYPE="LOGICAL" LABEL="Logical_Structure">
        <mets:div TYPE="UNITCOLLECTION" ID="UNITCOLLECTION_0001" LABEL="Výstřižky ke Klementinu" DMDID="MODSMD_UNITCOLLECTION_0001">{inner}
        </mets:div>
    </mets:structMap>'''

    page_divs = "".join(f'''
            <mets:div TYPE="normalPage" ID="DIV_PAGE_{i:04d}" ORDER="{i}" ORDERLABEL="{i}" DMDID="MODSMD_PAGE_{i:04d}">
                <mets:fptr FILEID="MC_{i:04d}"/>
                <mets:fptr FILEID="UC_{i:04d}"/>
                <mets:fptr FILEID="ALTO_{i:04d}"/>
                <mets:fptr FILEID="TXT_{i:04d}"/>
                <mets:fptr FILEID="AMD_METS_{i:04d}"/>
            </mets:div>''' for i in range(1, pages + 1))
    physical = f'''
    <mets:structMap TYPE="PHYSICAL" LABEL="Physical_Structure">
        <mets:div TYPE="UNIT" ID="DIV_P_0000" LABEL="{label}" DMDID="MODSMD_UNIT_0001">{page_divs}
        </mets:div>
    </mets:structMap>'''
    links = "".join(f'\n        <mets:smLink xlink:from="UNIT_0001" xlink:to="DIV_PAGE_{i:04d}"/>' for i in range(1, pages + 1))
    structlink = f'''
    <mets:structLink>{links}
    </mets:structLink>'''

    return f'''<?xml version="1.0" encoding="UTF-8"?>
<mets:mets {METS_NS} LABEL="{label}" TYPE="{mets_type}" xsi:schemaLocation="{SCHEMA_LOCATION}">
    <mets:metsHdr CREATEDATE="{CREATED}" LASTMODDATE="{CREATED}">
        <mets:agent ROLE="CREATOR" TYPE="ORGANIZATION">
            <mets:name>{SIGLA}</mets:name>
        </mets:agent>
        <mets:agent ROLE="ARCHIVIST" TYPE="ORGANIZATION">
            <mets:name>{SIGLA}</mets:name>
        </mets:agent>
    </mets:metsHdr>{dmd}
    <mets:fileSec>{filesec}
    </mets:fileSec>{logical}{physical}{structlink}
</mets:mets>
'''


def catalog_entry_xml(urnnbn: str) -> str:
    return f'''<?xml version="1.0" encoding="UTF-8"?>
<record xmlns="http://www.loc.gov/MARC21/slim">
    <leader>00000naa a2200000 i 4500</leader>
    <controlfield tag="001">000123456</controlfield>
    <controlfield tag="003">CZ PrNK</controlfield>
    <datafield tag="245" ind1="1" ind2="0">
        <subfield code="a">Klementinum a jeho knihovny :</subfield>
        <subfield code="b">k výročí Národní a universitní knihovny /</subfield>
        <subfield code="c">Jan Novák.</subfield>
    </datafield>
    <datafield tag="024" ind1="7" ind2=" ">
        <subfield code="a">{urnnbn}</subfield>
        <subfield code="2">urnnbn</subfield>
    </datafield>
</record>
'''


def info_xml(psp: str, items: list, size_kb: int, md5_name: str, md5_digest: str) -> str:
    item_els = "".join(f"\n        <item>\\{p.replace('/', chr(92))}</item>" for p in items)
    return f'''<?xml version="1.0" encoding="UTF-8"?>
<info>
    <created>{CREATED}</created>
    <metadataversion>0.1</metadataversion>
    <packageid>{psp}</packageid>
    <mainmets>mets_{psp}.xml</mainmets>
    <validation version="2.7">Valid</validation>
    <titleid type="uuid">{COLLECTION_UUID}</titleid>
    <titleid type="sysno">000123456</titleid>
    <collection>Výstřižky ke Klementinu</collection>
    <institution>Národní knihovna České republiky</institution>
    <creator>{SIGLA}</creator>
    <size>{size_kb}</size>
    <itemlist itemtotal="{len(items)}">{item_els}
    </itemlist>
    <checksum type="md5" checksum="{md5_digest}">\\{md5_name}</checksum>
    <note>Syntetický testovací balíček pro Komplexní validátor (gen_fund_unit_package.py).</note>
</info>
'''


# ----------------------------------------------------------------------------------------------------

MUTATIONS = {
    "missing-urnnbn": "UNIT bez identifikátoru urnnbn (MODS i DC) -> MODS_MANDATORY_IDENTIFIERS_PRESENT_LEVEL_UNIT, PSP_ID_DERIVED_FROM_IE_ID",
    "wrong-genre": "genre jednotky 'article' místo clipping/clippingIndex/cardIndex -> BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_UNIT",
    "bad-checksum": "špatný MD5 prvního MC v souboru .md5 -> CHECKSUM_FILE_ALL_CHECKSUMS_MATCH",
    "structlink-gap": "chybí smLink na poslední stranu -> PRIMARY-METS_STRUCT_LINKS_CORRECT",
    "itemtotal-mismatch": "itemtotal v INFO o 1 vyšší -> INFO_ITEMTOTAL_MATCHES_ITEMS_COUNT",
    "no-unit-dmdsec": "chybí dmdSec UNIT (zůstává UNITCOLLECTION) -> test kolize UNIT/UNITCOLLECTION; MODS_MANDATORY_IDENTIFIERS_PRESENT_LEVEL_UNIT, PSP_ID_DERIVED_FROM_IE_ID, mapy",
    "alto-mixed": "prvni strana ALTO 2.0, ostatni podle --alto-version -> OCR-ALTO_FILES_SAME_VERSION (WARNING)",
    "alto-unknown-version": "ALTO v neexistujicim namespace ns-v9 -> OCR-ALTO_FILES_VALID_BY_XSD (nepodporovana verze)",
    "wrong-mets-type": "mets/@TYPE 'Monograph' -> DmfDetector rozpozná jako monografii (balík se validuje jinou fDMF)",
    "wrong-dc-type": "dc:type jednotky 'model:monograph' -> BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_UNIT (DC)",
    "urnnbn-prohibited-on-collection": "urnnbn navíc na UNITCOLLECTION -> MODS_PROHIBITED_IDENTIFIERS_NOT_PRESENT_LEVEL_UNITCOLLECTION (WARNING)",
    "page-type-unknown": "TYPE stránky 'weirdPage' ve fyzické mapě -> PRIMARY-METS_PHYSICAL_STRUCTURAL_MAP_CORRECT (WARNING)",
}


def alto_version_for_page(page_no: int, alto_version: str, mutations: set) -> str:
    if "alto-unknown-version" in mutations:
        return "9.9"
    if "alto-mixed" in mutations and page_no == 1:
        return "2.0" if alto_version != "2.0" else "4.4"
    return alto_version


def generate(target: str, variant: str, pages: int, with_directory: bool, standard: str, mutations: set,
             jp2_mc: str = None, jp2_uc: str = None, urnnbn_override: str = None, unit_title: str = None,
             alto_version: str = "4.4"):
    unknown = mutations - set(MUTATIONS)
    if unknown:
        sys.exit("neznámé mutace: " + ", ".join(sorted(unknown)) + "; známé: " + ", ".join(sorted(MUTATIONS)))
    mets_type, genre, label, extent = VARIANTS[variant]
    if "wrong-mets-type" in mutations:
        mets_type = "Monograph"
    if "wrong-genre" in mutations:
        genre = "article"
    psp = os.path.basename(os.path.normpath(target))
    urnnbn = urnnbn_override or (f"urn:nbn:cz:{psp}" if not psp.count("-") == 4 else f"urn:nbn:cz:nk-00027x")
    global UNIT_TITLE
    if unit_title:
        UNIT_TITLE = unit_title
    os.makedirs(target, exist_ok=True)
    for d in ("mastercopy", "usercopy", "alto", "txt", "amdsec", "catalog_entry"):
        os.makedirs(os.path.join(target, d), exist_ok=True)

    written = {}  # relpath -> bytes

    def put(rel: str, data):
        if isinstance(data, str):
            data = data.encode("utf-8")
        written[rel] = data
        with open(os.path.join(target, rel), "wb") as f:
            f.write(data)
        return rel, len(data), md5_bytes(data)

    page_files = {}
    for i in range(1, pages + 1):
        p = f"{i:04d}"
        files = {
            "mc": put(f"mastercopy/mc_{psp}_{p}.jp2", open(jp2_mc, "rb").read() if jp2_mc else jp2_placeholder(f"mc{i}")),
            "uc": put(f"usercopy/uc_{psp}_{p}.jp2", open(jp2_uc, "rb").read() if jp2_uc else jp2_placeholder(f"uc{i}")),
            "alto": put(f"alto/alto_{psp}_{p}.xml", alto_xml(i, alto_version_for_page(i, alto_version, mutations))
                        .replace("{psp}", psp)),
            "txt": put(f"txt/txt_{psp}_{p}.txt", txt_content(i)),
        }
        files["amd"] = put(f"amdsec/amd_mets_{psp}_{p}.xml", amd_mets_xml(psp, mets_type, label, i, files))
        page_files[i] = files

    put(f"catalog_entry/cat_entry_{psp}.xml", catalog_entry_xml(urnnbn))
    mets = main_mets_xml(psp, mets_type, label, genre, extent, urnnbn, standard, pages, with_directory, page_files)
    if "missing-urnnbn" in mutations:
        mets = mets.replace(f'<mods:identifier type="urnnbn">{urnnbn}</mods:identifier>', "")
        mets = mets.replace(f"<dc:identifier>urnnbn:{urnnbn}</dc:identifier>", "")
    if "structlink-gap" in mutations:
        mets = mets.replace(f'<mets:smLink xlink:from="UNIT_0001" xlink:to="DIV_PAGE_{pages:04d}"/>', "")
    if "no-unit-dmdsec" in mutations:
        a = mets.index('<mets:dmdSec ID="MODSMD_UNIT_0001">'); b = mets.index('</mets:dmdSec>', mets.index('<mets:dmdSec ID="DCMD_UNIT_0001">')) + len('</mets:dmdSec>')
        mets = mets[:a] + mets[b:]
    if "wrong-dc-type" in mutations:
        mets = mets.replace("<dc:type>model:unit</dc:type>", "<dc:type>model:monograph</dc:type>")
    if "urnnbn-prohibited-on-collection" in mutations:
        mets = mets.replace(f'<mods:identifier type="uuid">{COLLECTION_UUID}</mods:identifier>',
                            f'<mods:identifier type="uuid">{COLLECTION_UUID}</mods:identifier>\n                        <mods:identifier type="urnnbn">urn:nbn:cz:nk-00099z</mods:identifier>')
    if "page-type-unknown" in mutations:
        mets = mets.replace('<mets:div TYPE="normalPage" ID="DIV_PAGE_0001"', '<mets:div TYPE="weirdPage" ID="DIV_PAGE_0001"')
    mets_rel = put(f"mets_{psp}.xml", mets)

    # MD5: vsechny soubory krome info a md5 (spec kap. 5.8), cesty absolutni vuci koreni balicku
    md5_name = f"md5_{psp}.md5"
    md5_lines = "".join(f"{md5_bytes(data)} /{rel}\r\n" for rel, data in sorted(written.items()))
    if "bad-checksum" in mutations:
        first_mc = f"mastercopy/mc_{psp}_0001.jp2"
        md5_lines = md5_lines.replace(md5_bytes(written[first_mc]), "0" * 32)
    md5_rel = put(md5_name, md5_lines)

    # INFO: itemlist vsech souboru vc. info.xml (spec kap. 5.1), size v kB bez info.xml
    info_name = f"info_{psp}.xml"
    items = sorted(written.keys()) + [info_name]
    size_kb = max(1, sum(len(d) for d in written.values()) // 1024)
    info = info_xml(psp, items, size_kb, md5_name, md5_rel[2])
    if "itemtotal-mismatch" in mutations:
        info = info.replace(f'itemtotal="{len(items)}"', f'itemtotal="{len(items) + 1}"')
    put(info_name, info)
    return target


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("target", help="cilovy adresar balicku; jeho nazev je PSP-ID (napr. .../nk-00027x)")
    ap.add_argument("--variant", choices=VARIANTS.keys(), default="clipping")
    ap.add_argument("--pages", type=int, default=2)
    ap.add_argument("--with-directory", action="store_true", help="pridat uroven DIRECTORY")
    ap.add_argument("--standard", choices=("aacr", "rda"), default="rda")
    ap.add_argument("--mutation", action="append", default=[], help="nazev mutace, viz --list-mutations")
    ap.add_argument("--list-mutations", action="store_true")
    ap.add_argument("--jp2-mc", help="skutecny JP2 soubor pouzity pro vsechny archivni kopie (misto placeholderu)")
    ap.add_argument("--jp2-uc", help="skutecny JP2 soubor pouzity pro vsechny uzivatelske kopie (misto placeholderu)")
    ap.add_argument("--urnnbn", help="URN:NBN jednotky (default odvozeny z nazvu balicku); PSP_ID pak nesedi, pouzit jen pro test Resolveru")
    ap.add_argument("--unit-title", help="nazev jednotky (mods:title / dc:title), napr. pro shodu s Resolverem")
    ap.add_argument("--alto-version", choices=("2.0", "3.0", "4.4"), default="4.4",
                    help="verze ALTO souboru (namespace); predpis OCR pripousti 2.0 a novejsi, fDMF fund_unit_0.1 ma XSD 2.0/3.1/4.4")
    a = ap.parse_args()
    if a.list_mutations:
        for k, v in MUTATIONS.items():
            print(f"{k}: {v}")
        return
    generate(a.target, a.variant, a.pages, a.with_directory, a.standard, set(a.mutation), a.jp2_mc, a.jp2_uc,
             a.urnnbn, a.unit_title, a.alto_version)
    print("OK:", a.target)


if __name__ == "__main__":
    main()
