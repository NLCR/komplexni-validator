package nkp.pspValidator.shared.metadataProfile;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class MetadataProfile {

    private String name;
    private String validatorVersion;
    private String dmf;

    private Map<String, String> namespaces;
    private Set<String> declaredDictionaries = Collections.emptySet();
    private ExpectedElementDefinition rootElementDefinition;

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }

    public Set<String> getDeclaredDictionaries() {
        return declaredDictionaries;
    }

    public void setDeclaredDictionaries(Set<String> declaredDictionaries) {
        this.declaredDictionaries = declaredDictionaries;
    }

    public ExpectedElementDefinition getRootElementDefinition() {
        return rootElementDefinition;
    }

    public void setRootElementDefinition(ExpectedElementDefinition rootElementDefinition) {
        this.rootElementDefinition = rootElementDefinition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValidatorVersion() {
        return validatorVersion;
    }

    public void setValidatorVersion(String validatorVersion) {
        this.validatorVersion = validatorVersion;
    }

    public String getDmf() {
        return dmf;
    }

    public void setDmf(String dmf) {
        this.dmf = dmf;
    }
}
