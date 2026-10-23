Testovací balíčky podle DMF Jednotky fondu 0.2 (draft 20. 10. 2026), vygenerované nástrojem
src/test/tools/gen_fund_unit_package.py s --standard-version 0.2 a reálnými JP2 z src/test/tools/jp2/:

  valid_clipping/nk-00027x             --standard-version 0.2
  valid_card_index_directory/nk-00027z --variant card_index --with-directory
  page_no_genre/nk-00027r              --mutation page-no-genre
  duplicate_page_uuid/nk-00027q        --pages 3 --mutation duplicate-page-uuid
  directory_dc_collection/nk-00027p    --with-directory --mutation directory-dc-collection
  origininfo_no_eventtype/nk-00027n    --mutation origininfo-no-eventtype
  unknown_identifier_type/nk-00027m    --mutation unknown-identifier-type

Vždy navíc --jp2-mc src/test/tools/jp2/mc.jp2 --jp2-uc src/test/tools/jp2/uc.jp2. Testy: FundUnitSmokeTest.
  unknown_identifier_type_invalid/nk-00027l --mutation unknown-identifier-type-invalid
  info_missing_collection_id/nk-00027k --mutation info-missing-collection-id
  top_div_type_unit_lowercase/nk-00027j --mutation top-div-type-unit-lowercase
  unit_dc_no_format/nk-00027i          --mutation unit-dc-no-format
  premis_format_not_mime/nk-00027h     --mutation premis-format-not-mime
  part_no_detail/nk-00027g             --mutation part-no-detail
  part_issue_only/nk-00027f            --mutation part-issue-only
