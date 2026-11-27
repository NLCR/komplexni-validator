package nkp.pspValidator.gui.settings;

import nkp.pspValidator.gui.newvalidation.DocumentTypes;
import nkp.pspValidator.shared.ConfigManifest;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.ValidatorConfigurationManager;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManagerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Ověření adresáře konfigurace validátoru vybraného v Nastavení (karta Konfigurace validátoru), dřív než se uloží.
 * Běžící aplikace se nemění: konfigurace se načte do samostatných dočasných objektů stejně jako při startu aplikace
 * (dialog inicializace validačních dat: {@link ValidatorConfigurationManager}, konfigurace externích nástrojů,
 * {@link FdmfRegistry}) a zahodí se. Navíc se vyžaduje aspoň jedna verze fDMF. Projde-li adresář tímto ověřením,
 * načte se po restartu aplikace stejně. Konfigurace jednotlivých fDMF se při startu nenačítají (až při prvním použití),
 * proto ani tady ne.
 * <p>
 * Bez JavaFX; čte soubory, proto volat mimo vlákno JavaFX.
 */
public final class ValidatorConfigCheck {

    /** Části konfigurace, které vyžaduje {@link ValidatorConfigurationManager} (v pořadí hlášení). */
    static final String[] REQUIRED_DIRS = {"fDMF", "dictionaries", "xsd"};
    static final String[] REQUIRED_FILES = {"externalUtils.xml", "xsd/fdmfConfig.xsd", "xsd/binaryFileProfile.xsd", "xsd/metadataProfile.xsd"};
    /** části přímo v adresáři konfigurace; chybí-li všechny, adresář konfiguraci vůbec neobsahuje */
    private static final List<String> TOP_LEVEL_PARTS = java.util.Arrays.asList("fDMF", "dictionaries", "xsd", "externalUtils.xml");
    /** název adresáře konfigurace v instalaci a ve zdrojích */
    static final String CONFIG_DIR_NAME = "validatorConfig";

    private ValidatorConfigCheck() {
    }

    /** Výsledek ověření: platný adresář, nebo důvod, proč ho nelze použít. */
    public static final class Result {
        private final String error;
        private final int fdmfCount;

        private Result(String error, int fdmfCount) {
            this.error = error;
            this.fdmfCount = fdmfCount;
        }

        static Result ok(int fdmfCount) {
            return new Result(null, fdmfCount);
        }

        static Result error(String error) {
            return new Result(error, 0);
        }

        public boolean isOk() {
            return error == null;
        }

        /** Důvod jako věta („Chybí podadresář fDMF.“), null u platného adresáře. */
        public String getError() {
            return error;
        }

        /** Počet verzí fDMF (všech typů dokumentů) v platném adresáři. */
        public int getFdmfCount() {
            return fdmfCount;
        }
    }

    /**
     * @param dir vybraný adresář
     * @param os  systém, pro který se načte konfigurace externích nástrojů (jako při startu)
     */
    public static Result check(File dir, OperatingSystem os) {
        if (dir == null || !dir.exists()) {
            return Result.error("Adresář neexistuje.");
        }
        if (!dir.isDirectory()) {
            return Result.error("Vybraná cesta není adresář.");
        }
        String[] children = dir.list();
        if (children == null) {
            return Result.error("Adresář nelze číst.");
        }
        if (children.length == 0) {
            return Result.error("Adresář je prázdný.");
        }
        List<String> missing = missingParts(dir);
        if (missing.containsAll(TOP_LEVEL_PARTS)) {
            File nested = new File(dir, CONFIG_DIR_NAME);
            if (nested.isDirectory() && missingParts(nested).isEmpty()) {
                return Result.error("Konfigurace je v podadresáři " + CONFIG_DIR_NAME + ", vyberte přímo ten.");
            }
            return Result.error("Adresář neobsahuje konfiguraci validátoru (podadresáře fDMF, dictionaries, xsd "
                    + "a soubor externalUtils.xml).");
        }
        //verze konfigurace jde před kontrolou úplnosti: konfigurace 2.x se pozná podle chybějícího manifestu a uživatel
        //má dostat hlášku o verzi, ne obecnější o chybějících částech
        ConfigManifest.Result manifest = ConfigManifest.evaluate(dir);
        if (manifest.getStatus().isError()) {
            return Result.error(manifest.getMessage());
        }
        if (!missing.isEmpty()) {
            return Result.error("Konfigurace není úplná, chybí: " + String.join(", ", missing) + ".");
        }
        try {
            ValidatorConfigurationManager manager = new ValidatorConfigurationManager(dir);
            new ExternalUtilManagerFactory(manager.getExternalUtilsConfigFile()).buildExternalUtilManager(os);
            FdmfRegistry registry = new FdmfRegistry(manager);
            int count = 0;
            for (Dmf.Type type : Dmf.Type.values()) {
                count += DocumentTypes.availableVersions(registry, type).size();
            }
            if (count == 0) {
                return Result.error("V podadresáři fDMF nejsou žádná pravidla validace (adresáře jako monograph_2.4).");
            }
            return Result.ok(count);
        } catch (ValidatorConfigurationException e) {
            return Result.error("Konfiguraci nelze načíst: " + e.getMessage());
        } catch (RuntimeException e) {
            //chybne XML konfigurace nastroju a podobne
            return Result.error("Konfiguraci nelze načíst: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    /** Chybějící povinné části (adresáře, pak soubory; soubory v chybějícím xsd se nehlásí zvlášť). */
    static List<String> missingParts(File dir) {
        List<String> missing = new ArrayList<>();
        for (String name : REQUIRED_DIRS) {
            if (!new File(dir, name).isDirectory()) {
                missing.add(name);
            }
        }
        boolean xsdMissing = missing.contains("xsd");
        for (String name : REQUIRED_FILES) {
            if (xsdMissing && name.startsWith("xsd/")) {
                continue;
            }
            if (!new File(dir, name).isFile()) {
                missing.add(name);
            }
        }
        return missing;
    }
}
