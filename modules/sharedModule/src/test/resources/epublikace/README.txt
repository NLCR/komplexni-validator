Testovací soubory a syntetické balíčky elektronických publikací (krok P5 plánu portu VEP).

Binární soubory (malé, používají je i balíčky):
  dummy_PDFA-1B.pdf        PDF/A-1b, výchozí archivní kopie PDF (--pdf)
  synteticky_epub3.epub    EPUB 3, výchozí archivní kopie EPUB (--epub)
  synteticky_bez_pdfa.pdf  PDF, které není PDF/A: původní soubor v originaldata (--od) a balíček pdf_not_pdfa
  (všechny tři používá také VfCheckBinaryFilesValidByExternalUtilTest)

Balíčky <fDMF>/<název>/<PSP-ID> vygenerované nástrojem src/test/tools/gen_epub_package.py (spouštět z
modules/sharedModule), příkaz: python3 src/test/tools/gen_epub_package.py src/test/resources/epublikace/<cesta> <parametry>.
Seznam mutací: --list-mutations. Testy: EpubSmokeTest (bez nástrojů), EpubWithToolsTest (veraPDF, EPUBCheck).
PSP-ID je název cílového adresáře: u e-monografie druhá část URN:NBN svazku, u e-periodik UUID čísla.

  emonograph_3.0/valid_pdf/aba007-p5m301                                           --type emo --version 3.0 --format pdf
  emonograph_3.0/valid_epub/aba007-p5m302                                          --type emo --version 3.0 --format epub
  emonograph_3.0/wrong_mets_type/aba007-p5m303                                     --type emo --version 3.0 --mutation wrong-mets-type
  emonograph_3.0/extra_file_in_original/aba007-p5m304                              --type emo --version 3.0 --mutation extra-file-in-original
  emonograph_3.1/valid_epub_originaldata/aba007-p5m311                             --type emo --version 3.1 --format epub --with-originaldata
  emonograph_3.1/originaldata_not_in_md5/aba007-p5m312                             --type emo --version 3.1 --with-originaldata --mutation originaldata-not-in-md5
  emonograph_3.1/supplement_dmdsec/aba007-p5m313                                   --type emo --version 3.1 --mutation supplement-dmdsec
  emonograph_3.1/volume_no_dc_creator/aba007-p5m314                                --type emo --version 3.1 --mutation volume-no-dc-creator
  emonograph_3.1/pdf_and_epub_both/aba007-p5m315                                   --type emo --version 3.1 --mutation pdf-and-epub-both
  emonograph_3.1/info_metadataversion_old/aba007-p5m316                            --type emo --version 3.1 --with-originaldata --mutation info-metadataversion-old
  emonograph_2.3/valid_pdf_rda/aba007-p5m231                                       --type emo --version 2.3 --format pdf
  emonograph_2.3/volume_no_dc_creator_rda/aba007-p5m232                            --type emo --version 2.3 --mutation volume-no-dc-creator
  eperiodical_2.5/valid_epub/5e0b0003-0000-4000-8000-000000000251                  --type epe --version 2.5 --format epub
  eperiodical_2.5/bad_checksum/5e0b0003-0000-4000-8000-000000000252                --type epe --version 2.5 --mutation bad-checksum
  eperiodical_2.5/missing_original_file/5e0b0003-0000-4000-8000-000000000253       --type epe --version 2.5 --mutation missing-original-file
  eperiodical_2.6/valid_pdf_originaldata/5e0b0003-0000-4000-8000-000000000261      --type epe --version 2.6 --format pdf --with-originaldata
  eperiodical_2.6/originaldata_bad_name/5e0b0003-0000-4000-8000-000000000262       --type epe --version 2.6 --with-originaldata --mutation originaldata-bad-name
  eperiodical_2.6/psp_id_from_title_id/5e0b0002-0000-4000-8000-000000000263        --type epe --version 2.6 --mutation psp-id-from-title-id
  eperiodical_2.6/volume_urnnbn/5e0b0003-0000-4000-8000-000000000264               --type epe --version 2.6 --mutation volume-urnnbn
  eperiodical_2.6/issue_no_urnnbn/5e0b0003-0000-4000-8000-000000000265             --type epe --version 2.6 --mutation issue-no-urnnbn
  eperiodical_2.6/itemtotal_mismatch/5e0b0003-0000-4000-8000-000000000266          --type epe --version 2.6 --mutation itemtotal-mismatch
  composed_perio_1.3/valid_a_pdf_originaldata/5e0b0003-0000-4000-8000-000000000131 --type sep --version 1.3 --format pdf --with-originaldata
  composed_perio_1.3/missing_amdsec/5e0b0003-0000-4000-8000-000000000132           --type sep --version 1.3 --with-originaldata --mutation missing-amdsec
  composed_perio_1.3/volume_urnnbn/5e0b0003-0000-4000-8000-000000000133            --type sep --version 1.3 --mutation volume-urnnbn
  composed_perio_1.3/oc_filegrp_ebgrp/5e0b0003-0000-4000-8000-000000000134         --type sep --version 1.3 --mutation oc-filegrp-ebgrp
  composed_perio_1.3/originaldata_bad_name/5e0b0003-0000-4000-8000-000000000135    --type sep --version 1.3 --with-originaldata --mutation originaldata-bad-name
  emonograph_3.0/pdf_not_pdfa/aba007-p5m305                                        --type emo --version 3.0 --pdf src/test/resources/epublikace/synteticky_bez_pdfa.pdf

Pozitivní balíčky jsou bez ERROR (Resolver URN:NBN a externí nástroje vypnuté). Co tvrdí jednotlivé testy, je
v komentářích EpubSmokeTest.
