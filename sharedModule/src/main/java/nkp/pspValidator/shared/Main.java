package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.utils.UrnNbnResolverChecker;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class Main {

    static UrnNbnResolverChecker checker = null;

    private static void init() throws KeyManagementException, NoSuchAlgorithmException {
        checker = new UrnNbnResolverChecker();
    }

    public static void main(String[] args) throws IOException, KeyStoreException, KeyManagementException, NoSuchAlgorithmException, UrnNbnResolverChecker.ResolverError {
        System.out.println("hello from shared module");
        init();
        //ACTIVE
        check("urn:nbn:cz:mzk-0001vw");
        //FREE
        check("urn:nbn:cz:mzk-xxxxxx");
        //RESERVED
        check("urn:nbn:cz:p01nk-000007");
        //DEACTIVATED
        check("urn:nbn:cz:zne001-00002o");

        //wrong lang code
        check("urn:nbn:cc:mzk-0001vw");
        //wrong prefix
        check("urn-nbn-cz:mzk-0001vw");
        //invalid registrar code
        check("urn:nbn:cz:m-0001vw");
        //invalid document code
        check("urn:nbn:cz:mzk-000");

    }

    private static void check(String urnNbn) throws IOException, KeyStoreException {
        try {
            checker.check(urnNbn);
        } catch (UrnNbnResolverChecker.ResolverError e) {
            System.out.println("ERROR: " + e.getMessage());
        } catch (UrnNbnResolverChecker.ResolverWarning e) {
            System.out.println("WARNING: " + e.getMessage());
        }
    }


}


