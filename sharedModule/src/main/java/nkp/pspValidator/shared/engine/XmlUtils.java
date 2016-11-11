package nkp.pspValidator.shared.engine;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 31.10.16.
 */
public class XmlUtils {


    public static List<Element> getChildrenElementsByName(Element root, String name) {
        NodeList nodes = root.getChildNodes();
        List<Element> result = new ArrayList<>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (element.getTagName().equals(name)) {
                    result.add(element);
                }
            }
        }
        return result;
    }


}
