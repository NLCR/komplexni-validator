Testovací balíčky podle DMF Jednotky fondu 0.2 (vygenerovány podle draftu z 20. 10. 2026, ověřeny proti finálnímu
textu NK z 2. 11. 2026), vygenerované nástrojem src/test/tools/gen_fund_unit_package.py. Každý příkaz má navíc vždy
--standard-version 0.2 --jp2-mc src/test/tools/jp2/mc.jp2 --jp2-uc src/test/tools/jp2/uc.jp2 (reálné JP2), uvedeny jsou
jen ostatní parametry. Testy: FundUnitSmokeTest.

  valid_clipping/nk-00027x             (bez dalších parametrů)
  valid_card_index_directory/nk-00027z --variant card_index --with-directory
  page_no_genre/nk-00027r              --mutation page-no-genre
  duplicate_page_uuid/nk-00027q        --pages 3 --mutation duplicate-page-uuid
  directory_dc_collection/nk-00027p    --with-directory --mutation directory-dc-collection
  origininfo_no_eventtype/nk-00027n    --mutation origininfo-no-eventtype
  unknown_identifier_type/nk-00027m    --mutation unknown-identifier-type
  unknown_identifier_type_invalid/nk-00027l --mutation unknown-identifier-type-invalid
  info_missing_collection_id/nk-00027k --mutation info-missing-collection-id
  top_div_type_unit_lowercase/nk-00027j --mutation top-div-type-unit-lowercase
  unit_dc_no_format/nk-00027i          --mutation unit-dc-no-format
  premis_format_not_mime/nk-00027h     --mutation premis-format-not-mime
  part_no_detail/nk-00027g             --mutation part-no-detail
  part_issue_only/nk-00027f            --mutation part-issue-only
  bad_checksum/nk-00027e               --mutation bad-checksum
  itemtotal_mismatch/nk-00027d         --mutation itemtotal-mismatch
  structlink_gap/nk-00027c             --mutation structlink-gap
  missing_urnnbn/nk-00027b             --mutation missing-urnnbn
  no_unit_dmdsec/nk-00027a             --mutation no-unit-dmdsec
  wrong_genre/nk-00026z                --mutation wrong-genre
  wrong_dc_type/nk-00026y              --mutation wrong-dc-type
  urnnbn_prohibited_on_collection/nk-00026x --mutation urnnbn-prohibited-on-collection
  page_type_unknown/nk-00026w          --mutation page-type-unknown
  wrong_mets_type/nk-00026v            --mutation wrong-mets-type
  detail_with_title_and_extent/nk-00026u --mutation detail-with-title-and-extent
  source_monograph_partnumber/nk-00026t --mutation source-monograph-partnumber
  page_generalview/nk-00026s           --mutation page-generalview
  logical_map_pages/nk-00026r          --mutation logical-map-pages
  valid_clipping_aacr/nk-00026q        --standard aacr
  valid_card_index/nk-00026p           --variant card_index
  valid_clipping_index/nk-00026n       --variant clipping_index
  valid_clipping_alto2/nk-00026m       --alto-version 2.0
  valid_clipping_alto3/nk-00026l       --alto-version 3.0
  detail_wrong_type/nk-00026k          --mutation detail-wrong-type
  page_genre_no_type/nk-00026j         --mutation page-genre-no-type
  page_dc_no_type/nk-00026i            --mutation page-dc-no-type
  amdmets_no_uc_filegrp/nk-00026h      --mutation amdmets-no-uc-filegrp
  extra_file_in_catalog_entry/nk-00026g --mutation extra-file-in-catalog-entry
  amdmets_no_agent/nk-00026f           --mutation amdmets-no-agent
  source_series_no_origininfo/nk-00026e --mutation source-series-no-origininfo
  info_no_uuid/nk-00026d               --mutation info-no-uuid
  unit_dc_extra_model/nk-00026c        --mutation unit-dc-extra-model
