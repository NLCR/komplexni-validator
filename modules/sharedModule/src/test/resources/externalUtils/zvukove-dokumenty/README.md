# Výstupy Jpylyzeru a JHOVE nad obrazy zvukových dokumentů

Skutečné výstupy nástrojů nad obrazovými kopiemi oficiálních vzorových balíčků NK pro test
`AudioImageRecommendationLevelsTest` (úrovně WARNING u kontrol, které DMF zvukových dokumentů uvádí jen jako
doporučení). Pořízeno 23. 11. 2026 na macOS (Apple Silicon).

- Jpylyzer 2.2.1 (`jpylyzer --version`), příkaz `jpylyzer jp2In <soubor>` jako spuštění `jp2k` v `externalUtils.xml`
  (`jp2In` Jpylyzer 2.2 bere jako další, neexistující soubor a vypíše na stderr „User warning: jp2In does not exist“).
- JHOVE 1.34.0 (`Jhove (Rel. 1.34.0, 2025-07-02)`), příkaz `jhove -h XML -m jpeg2000-hul -k <soubor>`.
- Spuštěno v adresáři souboru s relativní cestou; v `filePath` Jpylyzeru je místo absolutní cesty jen název souboru,
  jinak jsou výstupy beze změny.

| soubor | vstup | vlastnosti |
|---|---|---|
| `*_gram05_mc_0001.xml`, `*_gram05_uc_0001.xml` | `Balicek_dle_DMF_0.5_gramodesky.zip`, `mastercopy/mc_1234567890_0001.jp2`, `usercopy/uc_1234567890_0001.jp2` | 180 PPI (7086.61 px/m), parametry JPEG 2000 podle profilu |
| `*_fono_mc_0002.xml`, `*_fono_uc_0002.xml` | `vzorovy_balicek_fonograficke_valecky.zip`, `mc_…_0002.jp2`, `uc_…_0002.jp2` | bez údaje o rozlišení, jedna dlaždice 5000 × 3333, LRCP, 7 úrovní, 1 vrstva, precincts default, SOP, EPH a bypass ne |

Mezní hodnoty rozlišení (11732 a 11731,99 px/m) test vyrábí záměnou hodnoty ve výstupu gramodesky.
