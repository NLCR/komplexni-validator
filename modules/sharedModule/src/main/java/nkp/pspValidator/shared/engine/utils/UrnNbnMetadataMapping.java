package nkp.pspValidator.shared.engine.utils;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UrnNbnMetadataMapping {

    private final Map<String, List<UrnNbnMetadata>> result = new HashMap<>();

    public static class UrnNbnMetadata {
        public String type;
        public Node node;
    }

    public void addMetadata(String urnNbn, String type, Node node) {
        UrnNbnMetadata metadata = new UrnNbnMetadata();
        metadata.type = type;
        metadata.node = node;
        List<UrnNbnMetadata> list = result.get(urnNbn);
        if (list == null) {
            list = new ArrayList<>();
            result.put(urnNbn, list);
        }
        list.add(metadata);
    }

    public Node getMetadataByType(String urnNbn, String type) {
        List<UrnNbnMetadata> urnNbnMetadata = result.get(urnNbn);
        for (UrnNbnMetadata metadata : urnNbnMetadata) {
            if (metadata.type.equals(type)) {
                return metadata.node;
            }
        }
        return null;
    }

}
