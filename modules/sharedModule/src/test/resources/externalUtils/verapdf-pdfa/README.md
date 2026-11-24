# Výstupy veraPDF: soubor se k PDF/A hlásí, nebo nehlásí

Skutečné výstupy veraPDF pro test `VeraPdfPdfaClaimTest` (PDF, které se k PDF/A nehlásí, je WARNING; PDF/A, které
nevyhovuje, ERROR). Pořízeno 23. 11. 2026 na macOS (Apple Silicon), Homebrew `verapdf` 1.30.2 (`veraPDF 1.30.0`),
příkaz `verapdf <soubor>` jako spuštění `pdf` v `externalUtils.xml`. V `item/name` je místo absolutní cesty jen název
souboru, jinak jsou výstupy beze změny. Další výstupy veraPDF (PDF/A validní, prázdný soubor, text s příponou .pdf,
`dummy.pdf`) jsou v `../verapdf/`.

| soubor | vstup | kód | výsledek veraPDF |
|---|---|---|---|
| `nehlasi_se_xmp_bez_pdfaid_eperiodika_2.3.xml` | vzorek NK `eborn_periodika_2.3/PDF/e-periodika/AACR2/…/oc_…_0001.pdf` (shodný s RDA) | 1 | PDF/A-1b, mezi porušenými 6.7.11 test 1 (XMP bez identifikace PDF/A) |
| `nehlasi_se_xmp_bez_pdfaid_emonografie_2.3.xml` | vzorek NK `eborn_monografie_2.3/PDF/emono_kapitola/AACR2/…/oc_aba007-0000f8.pdf` | 1 | totéž |
| `nehlasi_se_bez_xmp.xml` | `src/test/resources/epublikace/synteticky_bez_pdfa.pdf` | 1 | PDF/A-1b, 6.7.2 test 1 (katalog bez Metadata) |
| `hlasi_se_pdfa_2b_nevyhovuje_eperiodika_2.4.xml` | vzorek NK `E-periodika 2.4/eborn_periodika_2.4 PDF/…/oc_….pdf` | 1 | PDF/A-2b, jen 6.2.4.4 test 2 |
| `poskozene_useknute.xml` | prvních 5000 B `dummy_PDFA-1B.pdf` | 7 | `taskException PARSE`, bez `validationReport` |
| `sifrovane_s_heslem.xml` | `dummy_PDFA-1B.pdf` zašifrované `qpdf --encrypt user owner 256` | 8 | `taskException PARSE` („appears to be encrypted“) |
| `sifrovane_bez_hesla_pdfa_1b.xml` | totéž s prázdným uživatelským heslem | 1 | PDF/A-1b, 6.1.7 test 2, 6.7.2 test 2, 6.1.3 test 2 |
| `synteticky_hlasi_se_pdfa_1b_metadata_bez_type.xml` | **syntetický vstup**: `dummy_PDFA-1B.pdf`, v QDF slovník metadat `/Type /Metadatx` (`qpdf --qdf`, `fix-qdf`) | 1 | PDF/A-1b, 6.1.7 test 1 a 6.7.2 test 1 (mez detekce) |
| `synteticky_hlasi_se_pdfa_2b_metadata_bez_type.xml` | **syntetický vstup**: totéž s `pdfaid:part` 2 | 1 | PDF/A-2b, 6.1.7.1 test 1 a 6.6.2.1 test 1 |
