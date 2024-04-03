package nkp.pspValidator.shared.engine.utils;

import org.json.JSONObject;
import org.json.JSONTokener;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class UrnNbnResolverChecker {

    private final SSLSocketFactory sslSocketFactory;

    public UrnNbnResolverChecker() throws NoSuchAlgorithmException, KeyManagementException {
        this.sslSocketFactory = initSslSocketFactory();
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

    public void check(String urnNbn) throws IOException, ResolverError, ResolverWarning {
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
        if (responseObj.has("urnNbn")) {
            JSONObject urnNbnObj = responseObj.getJSONObject("urnNbn");
            String status = urnNbnObj.getString("status");
            switch (status) {
                case "ACTIVE":
                    //OK
                    break;
                case "FREE":
                    throw new ResolverError("Identifikátor \"%s\" není registrován v Resolveru", urnNbn);
                case "RESERVED":
                    throw new ResolverWarning("Identifikátor \"%s\" zatím nebyl registrován v Resolveru, ale je rezervován", urnNbn);
                case "DEACTIVATED":
                    throw new ResolverWarning("Identifikátor \"%s\" sice byl registrován v Resolveru, ale nyní je deaktivován", urnNbn);
                default:
                    throw new ResolverError("Neznámý stav \"%s\" Resolveru pro Identifikátor \"%s\"", status, urnNbn);
            }

        } else {
            JSONObject urnNbnObj = responseObj.getJSONObject("error");
            String code = urnNbnObj.getString("code");
            String message = urnNbnObj.getString("message");
            if ("INTERNAL_ERROR".equals(code)) {
                message = message.split("\r\n|\r|\n", 2)[0];
            }
            throw new ResolverError(code + ": " + message);
        }
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


}
