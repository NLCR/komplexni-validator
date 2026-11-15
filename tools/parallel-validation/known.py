"""Známé rozdíly VEP 2.4 × KV 3.0 pro compare-vep.py.

Každé volání K(id, kategorie, popis, rule=, side=, level=, msg=) přidá jeden vzor; význam parametrů
popisuje compare-vep.py u tabulky KNOWN. První vyhovující vzor vyhrává, proto specifické vzory napřed.
"""


def register(K, INTENDED, VEP_FIX):
    # --- opravy chyb VEP (issues validator-e-publikaci) ---
    K('VEP #20: dmdSec MODSMD_SUPPL/DCMD_SUPPL v hlavičce METS', VEP_FIX,
      'VEP hlásí přílohové dmdSec jako neočekávaný element hlavičky METS (WARNING). KV je v profilu hlavičky '
      'povoluje jako nepovinné (P4, VEP issue #20).',
      rule=r'^PRIMARY_METS_HEADER_MATCHES_PROFILE$', side='vep',
      msg=r"neočekávaný element 'mets:dmdSec' \(ID=\"?(MODS|DC)MD_SUPPL")
    K('VEP #21: PSP-ID odvozené od identifikátorů intelektuální entity', VEP_FIX,
      'Pravidlo PSP_ID_DERIVED_FROM_TITLE_ID je v KV přejmenováno na PSP_ID_DERIVED_FROM_IE_ID, odvozuje PSP-ID '
      'od identifikátorů intelektuální entity (u přílohy a čísla jejich vlastní, ne titulu) a má úroveň ERROR '
      '(VEP WARNING). P4, VEP issue #21.',
      rule=r'^PSP_ID_DERIVED_FROM_IE_ID$')
    K('VEP #18: dc:creator svazku nepovinný v RDA', VEP_FIX,
      'VEP vyžaduje dc:creator u svazku i v RDA; KV ho podle DMF bere jako nepovinný (P4, VEP issue #18).',
      rule=r'^BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_', side='vep', msg=r"dc:creator")
    K('urnnbn u ročníku e-periodik', VEP_FIX,
      'VEP nepovoluje identifikátor urnnbn u ročníku (VOLUME) e-periodik a skládaných periodik; KV podle DMF ano (P4).',
      rule=r'^BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_VOLUME$', side='vep', msg=r"urnnbn")
    K('Překlep v XPath profilu přílohy e-monografií 2.4 (RDA)', VEP_FIX,
      'Profil supplement_rda.xml fDMF emonograph_2.4 ve VEP obsahuje XPath s chybějícím apostrofem '
      "(mods:part[@type='pageNumber]); VEP hlásí ERROR „chyba v zápisu Xpath“ a zbytek záznamu nezkontroluje. "
      'KV má překlep opravený (P4), takže místo toho hlásí skutečné chyby záznamu (např. recordChangeDate '
      've tvaru ...T12:35:0Z).',
      rule=r'^BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_SUPPLEMENT$', msg=r'chyba v zápisu Xpath')
    K('Překlep v XPath profilu přílohy e-monografií 2.4: následné hlášky KV', VEP_FIX,
      'Hlášky, které KV vydá díky opravenému XPath (viz předchozí položka) a VEP je kvůli výjimce nevydá.',
      rule=r'^BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_SUPPLEMENT$', side='kv', level=r'^(WARNING|ERROR)$',
      pkg=r'^E-monografie 2\.4_')
    K('R3: core media types EPUB 3.3 a oprava překlepu WOFF', VEP_FIX,
      'Profil EPUBCheck ve VEP má seznam povolených typů obsahu z r. 2021 s překlepem „application/font‐wof“ '
      '(U+2010), takže každý font WOFF dává WARNING „Kontrola typů obsažených objektů (Epub 3)“. KV má seznam '
      'podle core media types EPUB 3.3 (font/woff, font/woff2, application/font-woff, font/ttf, font/otf, '
      'application/font-sfnt, image/webp, audio/ogg; codecs=opus, JavaScript) a u EPUB 2 navíc DTBook a OEB 1.x.',
      rule=r'_VALID_BY_EPUBCHECK$', side='vep', msg=r'Kontrola typů obsažených objektů')
    K('R3: core media types EPUB 3.3, jiný výčet omezení v hlášce KV', VEP_FIX,
      'U typu, který zůstává nepovolený, se hláška KV liší od VEP jen vypsaným seznamem povolených hodnot '
      '(hláška obsahuje celý výčet omezení).',
      rule=r'_VALID_BY_EPUBCHECK$', side='kv', msg=r'Kontrola typů obsažených objektů')
    K('R1: úroveň PDF/A v obou velikostech písmen', VEP_FIX,
      'Profil veraPDF ve VEP očekává název profilu začínající velkým písmenem úrovně (PDF/A-2B), veraPDF 1.30 '
      'hlásí „PDF/A-2b validation profile“, takže VEP dává WARNING „Soubor PDF/A by měl být v podporované verzi“ '
      'u každého PDF. KV přijímá oba zápisy (P4, riziko R1).',
      rule=r'_VALID_BY_VERAPDF$', side='vep', msg=r'PDF/A')

    # --- úmyslné rozdíly KV ---
    K('KV #224: duplicitní identifikátory napříč dmdSec jako ERROR', INTENDED,
      'KV hlásí duplicitní identifikátor napříč MODS záznamy jako ERROR (issue KV #224, Java '
      'VfCheckIdentifiersNoDuplicates), VEP jako WARNING. Úroveň je natvrdo v Javě, ne ve fDMF.',
      rule=r'^MODS_IDENTIFIERS_NO_DUPLICATES_ACROSS_DMDSECS$', side='level', level=r'^WARNING>ERROR$')
    K('KV: doporučené elementy a atributy jako INFO', INTENDED,
      'Validátor metadat KV hlásí chybějící doporučené (nepovinné) elementy a atributy jako INFO; VEP je nehlásí. '
      'Týká se profilů hlavičky METS a bibliografických metadat.',
      side='kv', level=r'^INFO$', msg=r'doporučen')
    K('KV: vypnutý nástroj se hlásí jako „vypnutý“', INTENDED,
      'Při --disable-verapdf/--disable-epubcheck KV hlásí „nástroj X je vypnutý“, VEP „nástroj X není dostupný“ '
      '(obojí INFO; nález 27, P2).',
      rule=r'_VALID_BY_(VERAPDF|EPUBCHECK)$', level=r'^INFO$', msg=r'^nástroj \S+ (je vypnutý|není dostupný)$')
