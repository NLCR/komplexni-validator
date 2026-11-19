package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.ConfigProcessor;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Martin Řehánek on 2.11.16.
 * <p>
 * Registr fDMF z adresare validatorConfig/fDMF. Pri vytvoreni se jen projdou adresare fDMF (typ a verze z nazvu
 * adresare), konfigurace konkretni fDMF (FdmfConfiguration vcetne XSD validace konfiguracnich souboru, profilu
 * a kontroly validatorVersion) se nacte az pri prvnim getFdmfConfig a pak se drzi v registru. Chyba v konfiguraci
 * se proto ohlasi az pri prvnim pouziti dane fDMF, se stejnou hlaskou jako drive pri startu (ValidatorConfigurationException
 * z FdmfConfiguration). Kontrolu vsech vydavanych fDMF dela AllFdmfSmokeTest.
 * <p>
 * Pristup je vlaknove bezpecny (GUI pouziva registr z pracovnich vlaken).
 */
public class FdmfRegistry {

    private final ValidatorConfigurationManager validatorConfigManager;
    //adresare fDMF podle typu a verze; po konstruktoru se nemeni
    private final Map<Dmf.Type, Map<String, File>> fdmfDirsByTypeAndVersion = new EnumMap<>(Dmf.Type.class);
    //uz nactene konfigurace, pristup jen v synchronized metodach
    private final Map<Dmf, FdmfConfiguration> loadedFdmfConfigs = new HashMap<>();
    //sekce pravidel podle fDMF (jen pro cteni, ven se davaji kopie), pristup jen v synchronized metodach
    private final Map<Dmf, List<RulesSection>> rulesSectionsByDmf = new HashMap<>();
    private final Map<Dmf, String> rulesSectionsStampByDmf = new HashMap<>();

    public FdmfRegistry(ValidatorConfigurationManager validatorConfigManager) throws ValidatorConfigurationException {
        this.validatorConfigManager = validatorConfigManager;
        init();
    }

    private void init() {
        for (Dmf.Type type : Dmf.Type.values()) {
            fdmfDirsByTypeAndVersion.put(type, Collections.unmodifiableMap(findFdmfDirs(fdmfDirPrefix(type))));
        }
    }

    private Map<String, File> findFdmfDirs(String fdmfDirPefix) {
        Map<String, File> result = new HashMap<>();
        File[] fdmfDirs = validatorConfigManager.getFdmfDir().listFiles((dir, name) -> name.matches(fdmfDirPefix + "_[0-9]+(\\.([0-9])+)*"));
        for (File fdmfDir : fdmfDirs) {
            String versionNumber = fdmfDir.getName().substring(fdmfDirPefix.length() + 1);
            result.put(versionNumber, fdmfDir);
        }
        return result;
    }

    /**
     * Prefix nazvu adresare fDMF pro dany typ, napr. monograph pro monograph_2.3.
     */
    static String fdmfDirPrefix(Dmf.Type type) {
        switch (type) {
            case MONOGRAPH:
                return "monograph";
            case PERIODICAL:
                return "periodical";
            case AUDIO_GRAM:
                return "audio_gram";
            case AUDIO_FONO:
                return "audio_fono";
            case AUDIO_DISC:
                return "audio_disc";
            case AUDIO_NO_CARRIER:
                return "audio_no_carrier";
            case DATA_DISC:
                return "data_disc";
            case FUND_UNIT:
                return "fund_unit";
            case EMONOGRAPH:
                return "emonograph";
            case EPERIODICAL:
                return "eperiodical";
            case COMPERIODICAL:
                return "composed_perio";
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Pocet uz nactenych konfiguraci fDMF (pro testy).
     */
    public synchronized int getLoadedFdmfConfigCount() {
        return loadedFdmfConfigs.size();
    }

    /**
     * Zda je konfigurace dane fDMF uz nactena (pro testy).
     */
    public synchronized boolean isFdmfConfigLoaded(Dmf dmf) {
        return loadedFdmfConfigs.containsKey(dmf);
    }

    private Set<String> versions(Dmf.Type type) {
        return fdmfDirsByTypeAndVersion.get(type).keySet();
    }

    /**
     * Konfigurace fDMF, pri prvnim pozadavku se nacte a zvaliduje; null, pokud pro typ a verzi neexistuje adresar.
     */
    private synchronized FdmfConfiguration getOrLoad(Dmf.Type type, String version) throws ValidatorConfigurationException {
        File fdmfDir = version == null ? null : fdmfDirsByTypeAndVersion.get(type).get(version);
        if (fdmfDir == null) {
            return null;
        }
        Dmf dmf = new Dmf(type, version);
        FdmfConfiguration config = loadedFdmfConfigs.get(dmf);
        if (config == null) {
            config = new FdmfConfiguration(
                    fdmfDir,
                    validatorConfigManager.getFdmfConfigXsd(),
                    validatorConfigManager.getBinaryFileProfileXsd(),
                    validatorConfigManager.getMetadataProfileXsd(),
                    validatorConfigManager.getConfigSchemaCache());
            loadedFdmfConfigs.put(dmf, config);
        }
        return config;
    }


    public Set<String> getMonographFdmfVersions() {
        return versions(Dmf.Type.MONOGRAPH);
    }

    public Set<String> getPeriodicalFdmfVersions() {
        return versions(Dmf.Type.PERIODICAL);
    }

    public Set<String> getAudioGramFdmfVersions() {
        return versions(Dmf.Type.AUDIO_GRAM);
    }

    public Set<String> getAudioFonoFdmfVersions() {
        return versions(Dmf.Type.AUDIO_FONO);
    }

    public Set<String> getAudioDiscFdmfVersions() {
        return versions(Dmf.Type.AUDIO_DISC);
    }

    public Set<String> getAudioNoCarrierFdmfVersions() {
        return versions(Dmf.Type.AUDIO_NO_CARRIER);
    }

    public Set<String> getDataDiscFdmfVersions() {
        return versions(Dmf.Type.DATA_DISC);
    }

    public Set<String> getFundUnitFdmfVersions() {
        return versions(Dmf.Type.FUND_UNIT);
    }

    public Set<String> getEmonographFdmfVersions() {
        return versions(Dmf.Type.EMONOGRAPH);
    }

    public Set<String> getEperiodicalFdmfVersions() {
        return versions(Dmf.Type.EPERIODICAL);
    }

    public Set<String> getComposedPeriodicalFdmfVersions() {
        return versions(Dmf.Type.COMPERIODICAL);
    }


    public FdmfConfiguration getMonographFdmfConfig(String dmfVersion) throws ValidatorConfigurationException {
        return getOrLoad(Dmf.Type.MONOGRAPH, dmfVersion);
    }

    public FdmfConfiguration getPeriodicalFdmfConfig(String dmfVersion) throws ValidatorConfigurationException {
        return getOrLoad(Dmf.Type.PERIODICAL, dmfVersion);
    }

    public FdmfConfiguration getAudioGramFdmfConfig(String dmfVersion) throws ValidatorConfigurationException {
        return getOrLoad(Dmf.Type.AUDIO_GRAM, dmfVersion);
    }

    public FdmfConfiguration getAudioFonoFdmfConfig(String dmfVersion) throws ValidatorConfigurationException {
        return getOrLoad(Dmf.Type.AUDIO_FONO, dmfVersion);
    }

    public FdmfConfiguration getAudioDiscFdmfConfig(String dmfVersion) throws ValidatorConfigurationException {
        return getOrLoad(Dmf.Type.AUDIO_DISC, dmfVersion);
    }

    public FdmfConfiguration getAudioNoCarrierFdmfConfig(String dmfVersion) throws ValidatorConfigurationException {
        return getOrLoad(Dmf.Type.AUDIO_NO_CARRIER, dmfVersion);
    }

    public FdmfConfiguration getDataDiscFdmfConfig(String dmfVersion) throws ValidatorConfigurationException {
        return getOrLoad(Dmf.Type.DATA_DISC, dmfVersion);
    }

    public FdmfConfiguration getFundUnitFdmfConfig(String dmfVersion) throws ValidatorConfigurationException {
        return getOrLoad(Dmf.Type.FUND_UNIT, dmfVersion);
    }

    public FdmfConfiguration getEmonographFdmfConfig(String dmfVersion) throws ValidatorConfigurationException {
        return getOrLoad(Dmf.Type.EMONOGRAPH, dmfVersion);
    }

    public FdmfConfiguration getEperiodicalFdmfConfig(String dmfVersion) throws ValidatorConfigurationException {
        return getOrLoad(Dmf.Type.EPERIODICAL, dmfVersion);
    }

    public FdmfConfiguration getComposedPeriodicalFdmfConfig(String dmfVersion) throws ValidatorConfigurationException {
        return getOrLoad(Dmf.Type.COMPERIODICAL, dmfVersion);
    }


    /**
     * Sekce pravidel fDMF (id, nazev, popis) ve stejnem poradi jako v Engine postavenem pro tuto fDMF, bez nacteni
     * a validace cele konfigurace a bez stavby validatoru. Vysledek se cachuje, vraci se vzdy nove kopie sekci
     * (RulesSection je menitelna, viz setEnabled), takze se nesdileji mezi volajicimi.
     */
    public synchronized List<RulesSection> getRulesSections(Dmf dmf) throws UnknownFdmfException, ValidatorConfigurationException {
        File fdmfDir = dmf.getVersion() == null ? null : fdmfDirsByTypeAndVersion.get(dmf.getType()).get(dmf.getVersion());
        if (fdmfDir == null) {
            throw new UnknownFdmfException(dmf);
        }
        //otisk konfiguracnich souboru (cas zmeny a velikost): kdyz se za behu aplikace zmeni, sekce se nactou znovu,
        //aby dialog preskakovani ukazoval totez, co pouzije validace
        StringBuilder stampBuilder = new StringBuilder();
        for (String configFileName : FdmfConfiguration.CONFIG_FILE_NAMES) {
            File configFile = new File(fdmfDir, configFileName);
            stampBuilder.append(configFile.lastModified()).append(':').append(configFile.length()).append(';');
        }
        String stamp = stampBuilder.toString();
        List<RulesSection> sections = stamp.equals(rulesSectionsStampByDmf.get(dmf)) ? rulesSectionsByDmf.get(dmf) : null;
        if (sections == null) {
            sections = new ArrayList<>();
            for (String configFileName : FdmfConfiguration.CONFIG_FILE_NAMES) {
                File configFile = new File(fdmfDir, configFileName);
                FileUtils.checkFileExistAndReadable(configFile);
                sections.addAll(ConfigProcessor.readRulesSections(configFile));
            }
            rulesSectionsByDmf.put(dmf, sections);
            rulesSectionsStampByDmf.put(dmf, stamp);
        }
        List<RulesSection> copy = new ArrayList<>(sections.size());
        for (RulesSection section : sections) {
            copy.add(section.copy());
        }
        return copy;
    }

    public FdmfConfiguration getFdmfConfig(Dmf dmf) throws UnknownFdmfException, ValidatorConfigurationException {
        FdmfConfiguration config = getOrLoad(dmf.getType(), dmf.getVersion());
        if (config == null) {
            throw new UnknownFdmfException(dmf);
        }
        return config;
    }


    public static class UnknownFdmfException extends Exception {

        public UnknownFdmfException(Dmf dmf) {
            super(String.format("Není definována fDMF (formalizovaná DMF) pro: %s", dmf));
        }

    }

}
