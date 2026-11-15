Golden soubory protokolu validace (ProtocolGoldenTest)
=====================================================

Obsah: očekávaný textový protokol (<název>.txt, verbosity 3) a XML protokol (<název>.xml) pro pevné testovací balíčky:

  fund-unit-0.2-valid-clipping  fund_unit_0.2/valid_clipping/nk-00027x (fDMF Jednotky fondu 0.2, validní)
  fund-unit-0.2-page-no-genre   fund_unit_0.2/page_no_genre/nk-00027r (fDMF Jednotky fondu 0.2, nevalidní)
  monograph-1.3.2               monograph_1.3.2/b50eb6b0-f0a4-11e3-b72e-005056827e52 (fDMF monografie 1.3.2, nevalidní)
  audio-gram-0.5-no-ocr         odvozený za běhu z audio_gram_0.3/1234567890: kopie do dočasného adresáře, adresáře
                                přejmenované podle DMF 0.4+ (amdsec, mastercopy, usercopy, usercopy_audio), smazané alto
                                a txt, soubor md5 a itemlist v INFO přepsané na / a nové názvy bez OCR položek, aby
                                v protokolu nebyly cesty ani hlášky závislé na OS (fDMF gramodesky 0.5, nevalidní;
                                hlídá, že bez OCR nehlásí chybějící OCR adresáře ani PREMIS Object pro ALTO)

Jak vznikly: validace s vynucenou fDMF, všechny externí nástroje vypnuté (JHOVE, jpylyzer, ImageMagick, Kakadu, ...),
Resolver URN:NBN se nevolá (kv.skipUrnNbnResolver), XML protokol zapsaný Validator.run do dočasného adresáře.
Textový protokol proto končí i řádky o vytvoření XML exportu.

Normalizace před porovnáním (a před zápisem golden souborů):
  - absolutní cesty: [TMP] dočasný adresář XML protokolu, [BALIK] kořenový adresář balíčku, [MODUL] modules/sharedModule,
    [REPO] kořen repozitáře; ve Windows se zbytek cesty za zástupným řetězcem převede na /
  - verze validátoru a datum buildu: [VERZE], [BUILD_DATE] (patička textu, element validator v XML)
  - v XML časy startTime a finishTime: [CAS], délky durationMs: [MS]
  - konce řádků na \n
Namespace a schemaLocation XML protokolu obsahují verzi protokolu (3.0) a normalizované nejsou.

Přegenerování (z kořene repozitáře):

  ./gradlew :modules:sharedModule:test --tests '*ProtocolGoldenTest' -Dkv.updateGolden=true

(z IDE místo vlastnosti proměnná prostředí KV_UPDATE_GOLDEN=true). Testy v tomto režimu soubory přepíšou a projdou.

Změna hlášek, popisů pravidel, pořadí pravidel, fDMF nebo formátu protokolu se v těchto souborech projeví vždy. Je to
záměr: test hlídá, že se výstup nezměnil nechtěně. Po záměrné změně golden soubory přegeneruj a rozdíl projdi v git diff,
ať se do nich nedostane nechtěná změna.
