package nkp.pspValidator.shared.engine.utils;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UrnNbnMetadataMapping {

    private final Map<String, List<UrnNbnMetadata>> metadataByUrn = new HashMap<>();
    private final Map<String, Node> metadataById = new HashMap<>();


    public static class UrnNbnMetadata {
        public String entityType;
        public Node node;
    }

    public void addMetadataByUrn(String urnNbn, String entityType, Node node) {
        UrnNbnMetadata metadata = new UrnNbnMetadata();
        metadata.entityType = entityType;
        metadata.node = node;
        List<UrnNbnMetadata> list = metadataByUrn.get(urnNbn);
        if (list == null) {
            list = new ArrayList<>();
            metadataByUrn.put(urnNbn, list);
        }
        list.add(metadata);
    }

    public Node getMetadataByUrnAndEntityType(String urnNbn, String entityType) {
        List<UrnNbnMetadata> urnNbnMetadata = metadataByUrn.get(urnNbn);
        for (UrnNbnMetadata metadata : urnNbnMetadata) {
            if (metadata.entityType.equals(entityType)) {
                return metadata.node;
            }
        }
        return null;
    }

    public void addMetadataById(String id, Node node) {
        metadataById.put(id, node);
    }

    public Node getMetadataById(String id) {
        return metadataById.get(id);
    }

}
