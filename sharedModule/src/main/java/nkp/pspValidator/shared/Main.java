package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.utils.UrnNbnResolverChecker;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class Main {

    public static void main(String[] args) throws IOException, KeyStoreException, KeyManagementException, NoSuchAlgorithmException, UrnNbnResolverChecker.ResolverError {
        System.out.println("hello from shared module");
        //checkUrnNbns();
    }

    private static void checkUrnNbns() throws KeyManagementException, NoSuchAlgorithmException, IOException, KeyStoreException {
        UrnNbnResolverChecker checker = new UrnNbnResolverChecker();
        //init();
        //ACTIVE
        check(checker, "urn:nbn:cz:mzk-0001vw");
        //FREE
        check(checker, "urn:nbn:cz:mzk-xxxxxx");
        //RESERVED
        check(checker, "urn:nbn:cz:p01nk-000007");
        //DEACTIVATED
        check(checker, "urn:nbn:cz:zne001-00002o");

        //wrong lang code
        check(checker, "urn:nbn:cc:mzk-0001vw");
        //wrong prefix
        check(checker, "urn-nbn-cz:mzk-0001vw");
        //invalid registrar code
        check(checker, "urn:nbn:cz:m-0001vw");
        //invalid document code
        check(checker, "urn:nbn:cz:mzk-000");
    }

    private static void check(UrnNbnResolverChecker checker, String urnNbn) throws IOException, KeyStoreException {
        try {
            checker.check(urnNbn);
        } catch (UrnNbnResolverChecker.ResolverError e) {
            System.out.println("ERROR: " + e.getMessage());
        } catch (UrnNbnResolverChecker.ResolverWarning e) {
            System.out.println("WARNING: " + e.getMessage());
        }
    }


}


