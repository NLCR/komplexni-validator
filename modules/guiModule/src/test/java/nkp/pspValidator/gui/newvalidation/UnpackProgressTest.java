package nkp.pspValidator.gui.newvalidation;

import org.junit.Test;

import static org.junit.Assert.*;

/** Průběh rozbalování skupiny: text počtu souborů, podíl podle bajtů nebo souborů, texty panelu průběhu. */
public class UnpackProgressTest {

    private static final double EPS = 1e-9;

    @Test
    public void countsTextWithThousands() {
        assertEquals("1\u00A0234 z 5\u00A0678 souborů", new UnpackProgress(1234, 5678, 0, 0).countsText());
        assertEquals("0 z 1 souboru", new UnpackProgress(0, 1, 0, 0).countsText());
        assertEquals("1 z 1 souboru", new UnpackProgress(1, 1, 0, 0).countsText());
        assertEquals("2 z 2 souborů", new UnpackProgress(2, 2, 0, 0).countsText());
        assertEquals("0 z 0 souborů", new UnpackProgress(0, 0, 0, 0).countsText());
    }

    @Test
    public void numberGrouping() {
        assertEquals("0", UnpackProgress.number(0));
        assertEquals("999", UnpackProgress.number(999));
        assertEquals("1\u00A0000", UnpackProgress.number(1000));
        assertEquals("12\u00A0345", UnpackProgress.number(12345));
        assertEquals("123\u00A0456", UnpackProgress.number(123456));
        assertEquals("1\u00A0234\u00A0567", UnpackProgress.number(1234567));
    }

    @Test
    public void fractionByBytes() {
        //1 soubor z 4, ale polovina bajtu: rozhoduji bajty
        assertEquals(0.5, new UnpackProgress(1, 4, 50, 100).fraction(), EPS);
        assertEquals(0.0, new UnpackProgress(0, 4, 0, 100).fraction(), EPS);
        assertEquals(1.0, new UnpackProgress(4, 4, 100, 100).fraction(), EPS);
    }

    @Test
    public void fractionByFilesWhenSizeUnknownOrZero() {
        assertEquals(0.25, new UnpackProgress(1, 4, 999, -1).fraction(), EPS);
        //same prazdne soubory: velikost 0
        assertEquals(0.75, new UnpackProgress(3, 4, 0, 0).fraction(), EPS);
    }

    @Test
    public void fractionEdges() {
        assertEquals("archiv bez souborů je hotový", 1.0, new UnpackProgress(0, 0, 0, 0).fraction(), EPS);
        assertEquals(1.0, new UnpackProgress(0, 0, 0, -1).fraction(), EPS);
        //vic bajtu nez uvadi archiv (nespolehliva velikost): nejvys 1
        assertEquals(1.0, new UnpackProgress(1, 2, 150, 100).fraction(), EPS);
    }

    @Test
    public void panelTexts() {
        assertEquals("Rozbaluji skupinu…", UnpackProgress.groupTitle(null));
        assertEquals("Rozbaluji skupinu: 1\u00A0234 z 5\u00A0678 souborů", UnpackProgress.groupTitle(new UnpackProgress(1234, 5678, 1, 10)));
        assertEquals("před prvním hlášením neurčitý ukazatel", -1, UnpackProgress.groupFraction(null), EPS);
        assertEquals(0.1, UnpackProgress.groupFraction(new UnpackProgress(1234, 5678, 1, 10)), EPS);
    }
}
