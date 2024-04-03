package nkp.pspValidator.shared.engine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Martin Řehánek on 31.10.16.
 */
public class AbnfFuzzerTest {

    @Test
    public void abnfTest() {

        //Fuzzer fuzzer = new Fuzzer(new File("src/test/resources/abnf/windows_absolute.md5"));
        assertEquals(2, "neco:nic".split(":").length);
        assertEquals(2, "neco nic".split("\\s").length);
        assertEquals(2, "neco\tnic".split("\\s").length);
        assertEquals(1, "neconic".split("\\s").length);
        assertEquals(3, "neco\t\tnic".split("\\s").length);
        assertEquals(3, "neco\t nic".split("\\s").length);
        assertEquals(4, "neco   nic".split("\\s").length);

        assertEquals(2, "neco\tnic".split("\\t").length);
        assertEquals(1, "neco nic".split("\\t").length);

        assertEquals(2, "neco nic".split(" ").length);
        assertEquals(1, "neco\tnic".split(" ").length);

        assertEquals(2, "neco nic".split("[\\t ]").length);
        assertEquals(2, "neco\tnic".split("[\\t ]").length);


    }

}
