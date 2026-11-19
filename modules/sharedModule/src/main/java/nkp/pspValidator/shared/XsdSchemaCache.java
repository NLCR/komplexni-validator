package nkp.pspValidator.shared;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cache zkompilovanych XSD schemat podle souboru, aby se stejne XSD nekompilovalo znovu pro kazdy element nebo soubor.
 * <p>
 * Objekt Schema je podle specifikace JAXP vlaknove bezpecny a nemenny, cache ho proto sdili. Validator (Schema.newValidator())
 * vlaknove bezpecny neni, a proto si ho volajici vytvari pro kazde pouziti znovu. Neuspesna kompilace se necachuje,
 * dalsi pokus vyhodi stejnou vyjimku jako drive.
 * <p>
 * Schema se kompiluje bud bez resolveru importu (jako SchemaFactory.newSchema(file)), nebo s XsdImportsResourceResolver
 * nad adresarem XSD souboru. Kazda varianta ma v cache vlastni zaznam, protoze vysledek kompilace se muze lisit.
 * <p>
 * Klic obsahuje i cas posledni zmeny a velikost souboru: kdyz se XSD za behu aplikace zmeni (GUI drzi cache mezi
 * validacemi), zkompiluje se znovu. Zmena importovaneho schematu beze zmeny hlavniho souboru se takto nepozna.
 */
public class XsdSchemaCache {

    private final ConcurrentMap<Key, Schema> cache = new ConcurrentHashMap<>();
    private final AtomicInteger compilations = new AtomicInteger();

    /**
     * Schema zkompilovane jako SchemaFactory.newSchema(xsdFile), bez vlastniho resolveru importu.
     */
    public Schema getSchema(File xsdFile) throws SAXException {
        return get(xsdFile, false);
    }

    /**
     * Schema zkompilovane s XsdImportsResourceResolver nad adresarem XSD souboru (importy se hledaji vedle XSD).
     */
    public Schema getSchemaWithImportsResolver(File xsdFile) throws SAXException {
        return get(xsdFile, true);
    }

    /**
     * Pocet skutecnych kompilaci schematu (pro testy).
     */
    public int getCompilationCount() {
        return compilations.get();
    }

    private Schema get(File xsdFile, boolean importsResolver) throws SAXException {
        //null se necachuje: vyjimka (NPE) pak vznikne ve stejnem vyrazu jako pred zavedenim cache
        Key key = xsdFile == null ? null : new Key(xsdFile.getAbsolutePath(), importsResolver, xsdFile.lastModified(), xsdFile.length());
        Schema schema = key == null ? null : cache.get(key);
        if (schema == null) {
            //kompilace mimo zamek; pri soubehu se schema muze zkompilovat dvakrat, pouzije se prvni ulozene
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            if (importsResolver) {
                schemaFactory.setResourceResolver(new XsdImportsResourceResolver(xsdFile.getParentFile()));
            }
            Schema compiled = schemaFactory.newSchema(xsdFile);
            compilations.incrementAndGet();
            if (key == null) {
                return compiled;
            }
            Schema previous = cache.putIfAbsent(key, compiled);
            schema = previous != null ? previous : compiled;
        }
        return schema;
    }

    private static final class Key {
        private final String path;
        private final boolean importsResolver;
        private final long lastModified;
        private final long length;

        Key(String path, boolean importsResolver, long lastModified, long length) {
            this.path = path;
            this.importsResolver = importsResolver;
            this.lastModified = lastModified;
            this.length = length;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return importsResolver == key.importsResolver && lastModified == key.lastModified && length == key.length && path.equals(key.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, importsResolver, lastModified, length);
        }
    }
}
