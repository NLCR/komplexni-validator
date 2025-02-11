package nkp.pspValidator.shared.engine.utils;

import nkp.pspValidator.shared.NamespaceContextImpl;
import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.net.ssl.*;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class UrnNbnResolverChecker {

    private final SSLSocketFactory sslSocketFactory;
    private final UrnNbnMetadataMapping metadataByUrnNbn;

    public UrnNbnResolverChecker(File metsFile) throws NoSuchAlgorithmException, KeyManagementException {
        this.sslSocketFactory = initSslSocketFactory();
        this.metadataByUrnNbn = extractMetadataByUrnNbn(metsFile);
    }

    private UrnNbnMetadataMapping extractMetadataByUrnNbn(File metsFile) {
        //System.out.println("extracting metadata by URN:NBN from METS file");
        //System.out.println("mets file: " + metsFile.getAbsolutePath());
        try {
            UrnNbnMetadataMapping metadataMapping = new UrnNbnMetadataMapping();
            Document metsDoc = XmlUtils.buildDocumentFromFile(metsFile, true);
            //System.out.println(XmlUtils.toString(metsDoc).substring(0, 500));
            NodeList modsEls = (NodeList) buildXpath("//mods:mods").evaluate(metsDoc, XPathConstants.NODESET);
            for (int i = 0; i < modsEls.getLength(); i++) {
                Element element = (Element) modsEls.item(i);
                String id = element.getAttribute("ID");
                if (id != null && !id.isEmpty()) {
                    String type = id.split("_")[1];
                    NodeList modsIdEls = (NodeList) buildXpath("//mods:identifier[@type='urnnbn']").evaluate(metsDoc, XPathConstants.NODESET);
                    if (modsIdEls.getLength() != 0) {
                        String urnNbn = modsIdEls.item(0).getTextContent();
                        metadataMapping.addMetadata(urnNbn, type, element);
                    }
                }
            }
            return metadataMapping;
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return null;
        }
    }

    private XPathExpression buildXpath(String xpathExpression) throws InvalidXPathExpressionException {
        try {
            NamespaceContextImpl namespaceContext = new NamespaceContextImpl();
            namespaceContext.setNamespace("mets", "http://www.loc.gov/METS/");
            namespaceContext.setNamespace("mods", "http://www.loc.gov/mods/v3");
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            xpath.setNamespaceContext(namespaceContext);
            return xpath.compile(xpathExpression);
        } catch (XPathExpressionException e) {
            throw new InvalidXPathExpressionException(xpathExpression, String.format("chyba v zápisu Xpath '%s': %s", xpathExpression, e.getMessage()));
        }
    }

    /**
     * This SSLSocketFactory does not check SSL certificates at all and accepts everything.
     * So there's no protection against man-in-the-middle attacks.
     * But it wouldn't be serious problem, if false resolver server claimed that some identifiers were or were not registered, etc.
     * It would be much more challenging to keep current CA certificates for Terena, Let's encrypt or whoever may NKP decide to use as CA in the future.
     *
     * @return
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    private SSLSocketFactory initSslSocketFactory() throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] trustManager = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String t) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String t) {
            }
        }};
        sslContext.init(null, trustManager, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    public void check(String urnNbn) throws IOException, ResolverError, ResolverWarning, XPathExpressionException, InvalidXPathExpressionException {
        //if (true) throw new IOException("TEMPORARILY DISABLED");
        //System.err.println("checking URN:NBN: " + urnNbn);
        JSONObject urnNbnStatusObj = getUrnNbnStatus(urnNbn);
        if (urnNbnStatusObj.has("urnNbn")) {
            JSONObject urnNbnObj = urnNbnStatusObj.getJSONObject("urnNbn");
            String status = urnNbnObj.getString("status");
            switch (status) {
                case "FREE":
                    throw new ResolverError("Identifikátor \"%s\" není registrován v Resolveru", urnNbn);
                case "RESERVED":
                    throw new ResolverWarning("Identifikátor \"%s\" zatím nebyl registrován v Resolveru, ale je rezervován", urnNbn);
                case "DEACTIVATED":
                    throw new ResolverWarning("Identifikátor \"%s\" sice byl registrován v Resolveru, ale nyní je deaktivován", urnNbn);
                default:
                    throw new ResolverError("Neznámý stav \"%s\" Resolveru pro Identifikátor \"%s\"", status, urnNbn);
                case "ACTIVE":
                    if (metadataByUrnNbn != null) {
                        try {
                            checkMetadataMatch(urnNbn);
                        } catch (MetadataMismatchException e) {
                            throw new ResolverWarning(e.getMessage());
                        }
                    } else {
                        //OK, no metadata checking
                    }
                    break;
            }
        } else {
            JSONObject urnNbnObj = urnNbnStatusObj.getJSONObject("error");
            String code = urnNbnObj.getString("code");
            String message = urnNbnObj.getString("message");
            if ("INTERNAL_ERROR".equals(code)) {
                message = message.split("\r\n|\r|\n", 2)[0];
            }
            throw new ResolverError(code + ": " + message);
        }
    }

    private void checkMetadataMatch(String urnNbn) throws IOException, XPathExpressionException, InvalidXPathExpressionException, MetadataMismatchException {
        JSONObject digDocMetadata = getDigDocMetadata(urnNbn);
        //System.out.println(digDocMetadata.toString(2));
        JSONObject digitalDocument = digDocMetadata.getJSONObject("digitalDocument");
        JSONObject titleInfo = null;

        if (digitalDocument.has("periodical")) { //https://resolver.nkp.cz/api/v5/resolver/urn:nbn:cz:bve302-000004?format=json
            titleInfo = digitalDocument.getJSONObject("periodical").getJSONObject("titleInfo");
            System.out.println("TODO: check PERIODICAL");
        } else if (digitalDocument.has("periodicalVolume")) { //https://resolver.nkp.cz/api/v5/resolver/urn:nbn:cz:bve302-000005?format=json
            titleInfo = digitalDocument.getJSONObject("periodicalVolume").getJSONObject("titleInfo");
            System.out.println("TODO: check PERIODICAL VOLUME");
        } else if (digitalDocument.has("periodicalIssue")) {//https://resolver.nkp.cz/api/v6/resolver/urn:nbn:cz:abg001-0005lt?format=json
            titleInfo = digitalDocument.getJSONObject("periodicalIssue").getJSONObject("titleInfo");
            System.out.println("TODO: check PERIODICAL ISSUE");
        } else if (digitalDocument.has("monograph")) { //https://resolver.nkp.cz/api/v6/resolver/urn:nbn:cz:mzk-006obk?format=json
            titleInfo = digitalDocument.getJSONObject("monograph").getJSONObject("titleInfo");
            System.out.println("TODO: check MONOGRAPH");
        } else if (digitalDocument.has("monographVolume")) { //https://resolver.nkp.cz/api/v5/resolver/urn:nbn:cz:nk-003do9?format=json
            titleInfo = digitalDocument.getJSONObject("monographVolume").getJSONObject("titleInfo");
            checkMetadata("MONOGRAPH_VOLUME", titleInfo, urnNbn);
        } else if (digitalDocument.has("thesis")) { //https://resolver.nkp.cz/api/v5/resolver/urn:nbn:cz:bve302-000007?format=json
            titleInfo = digitalDocument.getJSONObject("thesis").getJSONObject("titleInfo");
            System.out.println("TODO: check THESIS");
        } else if (digitalDocument.has("analytical")) { //https://resolver.nkp.cz/api/v6/resolver/urn:nbn:cz:pna001-00cxz5?format=json
            titleInfo = digitalDocument.getJSONObject("analytical").getJSONObject("titleInfo");
            System.out.println("TODO: check ANALYTICAL");
        } else if (digitalDocument.has("otherEntity")) { //https://resolver.nkp.cz/api/v5/resolver/urn:nbn:cz:abg001-0003ig?format=json
            titleInfo = digitalDocument.getJSONObject("otherEntity").getJSONObject("titleInfo");
            System.out.println("TODO: check OTHER ENTITY");
        } else {
            //unexpected data structure
            System.err.println("ERROR: unexpected data structure in digDocMetadata");
        }
    }

    private void checkMetadata(String czidlo_type, JSONObject titleInfo, String urnNbn) throws XPathExpressionException, IOException, InvalidXPathExpressionException, MetadataMismatchException {
        //System.out.println("checking metadata for " + urnNbn + ", type: " + czidlo_type);
        switch (czidlo_type) {
            case "MONOGRAPH_VOLUME":
                String modsType = "VOLUME";
                Node modsMetadata = metadataByUrnNbn.getMetadataByType(urnNbn, "VOLUME");
                checkMonographVolumeMetadata(czidlo_type, modsType, modsMetadata, titleInfo, urnNbn);
                break;
            default:
                System.out.println("TODO: implement checking for " + czidlo_type);
        }
    }

    private void checkMonographVolumeMetadata(String czidlo_type, String modsType, Node modsMetadata, JSONObject titleInfo, String urnNbn) throws IOException, InvalidXPathExpressionException, XPathExpressionException, MetadataMismatchException {
        if (modsMetadata != null) {
            /*
            System.out.println(titleInfo.toString(2));
            try {
                System.out.println(XmlUtils.elementToString((Element) modsMetadata));
            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
            }
            */
            checkDataMatch(
                    (String) buildXpath("mods:titleInfo/mods:title").evaluate(modsMetadata, XPathConstants.STRING),
                    titleInfo.getString("monographTitle"), urnNbn, czidlo_type, modsType
            );
            checkDataMatch(
                    (String) buildXpath("mods:titleInfo/mods:partNumber").evaluate(modsMetadata, XPathConstants.STRING),
                    titleInfo.getString("volumeTitle"), urnNbn, czidlo_type, modsType
            );
        } else {
            System.out.println("MODS metadata not found for " + urnNbn);
        }
    }

    private void checkDataMatch(String first, String second, String urnNbn, String czidloType, String modsType) throws MetadataMismatchException {
        if (differs(first, second)) {
            throw new MetadataMismatchException(urnNbn, czidloType, modsType, first, second);
        } else {
            System.out.println("OK: metadata for " + urnNbn + " matches");
        }
    }

    private boolean differs(String first, String second) {
        boolean differ = first == null && second != null || first != null && !first.equals(second);
        /*if (!differ) {
            System.out.println("first: " + first);
            System.out.printf("second: " + second);
            System.out.println("differs: " + differ);
        }*/
        return differ;
        //return true;
    }

    private JSONObject getDigDocMetadata(String urnNbn) throws IOException {
        //URL url = new URL(String.format("https://resolver-dev.nkp.cz/api/v5/resolver/%s?format=json", urnNbn));
        URL url = new URL(String.format("https://resolver.nkp.cz/api/v5/resolver/%s?format=json", urnNbn));
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setSSLSocketFactory(sslSocketFactory);
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setInstanceFollowRedirects(true);

        InputStream responseStream = null;
        if (con.getResponseCode() == 200) {
            responseStream = con.getInputStream();
        } else {
            responseStream = con.getErrorStream();
        }
        JSONTokener tokener = new JSONTokener(responseStream);
        JSONObject responseObj = new JSONObject(tokener);
        return responseObj;
    }

    private JSONObject getUrnNbnStatus(String urnNbn) throws IOException {
        //URL url = new URL(String.format("https://resolver-dev.nkp.cz/api/v5/urnnbn/%s?format=json", urnNbn));
        URL url = new URL(String.format("https://resolver.nkp.cz/api/v5/urnnbn/%s?format=json", urnNbn));
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setSSLSocketFactory(sslSocketFactory);
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setInstanceFollowRedirects(true);

        InputStream responseStream = null;
        if (con.getResponseCode() == 200) {
            responseStream = con.getInputStream();
        } else {
            responseStream = con.getErrorStream();
        }
        JSONTokener tokener = new JSONTokener(responseStream);
        JSONObject responseObj = new JSONObject(tokener);
        return responseObj;
    }


    public static class ResolverError extends Exception {

        public ResolverError(String message) {
            super(message);
        }

        public ResolverError(String messageTemplate, Object... params) {
            super(String.format(messageTemplate, params));
        }

    }

    public static class ResolverWarning extends Exception {

        public ResolverWarning(String messageTemplate, Object... params) {
            super(String.format(messageTemplate, params));
        }
    }

    private static class MetadataMismatchException extends Exception {

        private final String urnNbn;
        private final String czidloType;
        private final String modsType;
        private final String first;
        private final String second;

        public MetadataMismatchException(String urnNbn, String czidloType, String modsType, String first, String second) {
            this.urnNbn = urnNbn;
            this.czidloType = czidloType;
            this.modsType = modsType;
            this.first = first;
            this.second = second;
        }

        public String getMessage() {
            return String.format("metadata mismatch for %s: '%s' != '%s' (CZIDLO.type:%s, MODS.type:%s)", urnNbn, first, second, czidloType, modsType);
        }
    }
}
