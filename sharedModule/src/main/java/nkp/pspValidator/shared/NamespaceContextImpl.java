package nkp.pspValidator.shared;

import javax.xml.namespace.NamespaceContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by martin on 17.11.16.
 */
public class NamespaceContextImpl implements NamespaceContext {
    private final Map<String, String> namespaceByPrefix = new HashMap<>();

    public void setNamespace(String prefix, String uri) {
        if (namespaceByPrefix.containsKey(prefix)) {
            throw new IllegalStateException(String.format("prefix '%s' je již registrován a to pro uri '%s'", prefix, namespaceByPrefix.get(prefix)));
        } else {
            namespaceByPrefix.put(prefix, uri);
        }
    }


    @Override
    public String getNamespaceURI(String prefix) {
        String uri = namespaceByPrefix.get(prefix);
        return uri;
    }

    @Override
    public String getPrefix(String namespaceURI) {
        if (namespaceByPrefix.values().contains(namespaceURI)) {
            for (String prefix : namespaceByPrefix.keySet()) {
                if (namespaceByPrefix.get(prefix).equals(namespaceURI)) {
                    return prefix;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        return namespaceByPrefix.keySet().iterator();
    }

}
