package nkp.pspValidator.shared.biblio.biblioValidator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class BiblioTemplate {

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
}
