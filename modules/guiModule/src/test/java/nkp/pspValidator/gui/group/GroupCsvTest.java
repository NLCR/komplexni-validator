package nkp.pspValidator.gui.group;

import nkp.pspValidator.gui.results.ProblemCounts;
import nkp.pspValidator.shared.Dmf;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Export souhrnu skupiny do CSV: BOM, středník, CRLF, uvozování polí se středníkem, uvozovkou a koncem řádku, prázdné
 * hodnoty. Očekávané řádky jsou napsané ručně.
 */
public class GroupCsvTest {

    private static final String HEADER = "Balík;Cesta;Typ;Verze DMF;Verdikt;Chyby;Varování;Informace;Doba (s);Protokol XML;Textový log;Chybová hláška";

    private static GroupRun run(String... names) {
        List<GroupPackages.Entry> entries = new ArrayList<>();
        for (String name : names) {
            entries.add(new GroupPackages.Entry(new File("/data/davka", name), name.endsWith(".zip")));
        }
        return new GroupRun(new GroupPackages.Listing(new File("/data/davka"), entries, Collections.<String>emptyList()));
    }

    private static byte[] write(List<GroupItem> items) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GroupCsv.write(items, out);
        return out.toByteArray();
    }

    private static String withoutBom(byte[] bytes) {
        assertTrue("BOM", bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF);
        return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
    }

    @Test
    public void emptyGroupHasOnlyHeader() throws IOException {
        assertEquals(HEADER + "\r\n", withoutBom(write(Collections.<GroupItem>emptyList())));
    }

    @Test
    public void rowsForAllStates() throws IOException {
        GroupRun run = run("a", "b.zip", "c", "d");
        GroupItem a = run.getItems().get(0);
        GroupItem b = run.getItems().get(1);
        GroupItem c = run.getItems().get(2);
        run.runStarted(run.getItems(), 0);
        run.packageStarted(a);
        run.packageFinished(a, new Dmf(Dmf.Type.MONOGRAPH, "2.2"), new ProblemCounts(12, 3, 0), false, 12345,
                new File("/logy/a.xml"), new File("/logy/a.txt"));
        run.packageStarted(b);
        run.packageFailed(b, null, null, "nenalezen soubor PRIMARY-METS", 49);
        run.packageStarted(c);
        run.packageFinished(c, new Dmf(Dmf.Type.FUND_UNIT, "0.2"), new ProblemCounts(0, 1, 139), true, 60_000, null, null);
        run.cancelRequested();
        run.runEnded(70_000, java.time.LocalDateTime.now());

        String csv = withoutBom(write(run.getItems()));
        String a1 = new File("/data/davka/a").getAbsolutePath();
        String expected = HEADER + "\r\n"
                + "a;" + a1 + ";Monografie;2.2;nevalidní;12;3;0;12,3;" + new File("/logy/a.xml").getAbsolutePath() + ";"
                + new File("/logy/a.txt").getAbsolutePath() + ";\r\n"
                + "b.zip;" + new File("/data/davka/b.zip").getAbsolutePath() + ";;;chyba;;;;0,0;;;nenalezen soubor PRIMARY-METS\r\n"
                + "c;" + new File("/data/davka/c").getAbsolutePath() + ";Jednotky fondu;0.2;validní;0;1;139;60,0;;;\r\n"
                + "d;" + new File("/data/davka/d").getAbsolutePath() + ";;;nevalidováno;;;;;;;\r\n";
        assertEquals(expected, csv);
    }

    @Test
    public void fieldsWithSeparatorQuoteAndNewlineAreQuoted() throws IOException {
        GroupRun run = run("x;y", "rec \"nazev\"");
        GroupItem first = run.getItems().get(0);
        GroupItem second = run.getItems().get(1);
        run.runStarted(run.getItems(), 0);
        run.packageStarted(first);
        run.packageFailed(first, null, null, "řádek 1\nřádek 2; \"citace\"", 1000);
        run.packageStarted(second);
        run.packageFailed(second, null, null, "konec\r\n", 1000);
        run.runEnded(2000, java.time.LocalDateTime.now());

        String csv = withoutBom(write(run.getItems()));
        String path1 = new File("/data/davka/x;y").getAbsolutePath();
        String path2 = new File("/data/davka/rec \"nazev\"").getAbsolutePath();
        String expected = HEADER + "\r\n"
                + "\"x;y\";\"" + path1 + "\";;;chyba;;;;1,0;;;\"řádek 1\nřádek 2; \"\"citace\"\"\"\r\n"
                + "\"rec \"\"nazev\"\"\";\"" + path2.replace("\"", "\"\"") + "\";;;chyba;;;;1,0;;;\"konec\r\n\"\r\n";
        assertEquals(expected, csv);
    }

    @Test
    public void field() {
        assertEquals("", GroupCsv.field(null));
        assertEquals("", GroupCsv.field(""));
        assertEquals("prosty text, s carkou", GroupCsv.field("prosty text, s carkou"));
        assertEquals("\"a;b\"", GroupCsv.field("a;b"));
        assertEquals("\";\"", GroupCsv.field(";"));
        assertEquals("\"\"\"\"", GroupCsv.field("\""));
        assertEquals("\"rekl \"\"ahoj\"\"\"", GroupCsv.field("rekl \"ahoj\""));
        assertEquals("\"a\nb\"", GroupCsv.field("a\nb"));
        assertEquals("\"a\rb\"", GroupCsv.field("a\rb"));
    }

    @Test
    public void seconds() {
        assertEquals("0,0", GroupCsv.seconds(0));
        assertEquals("0,0", GroupCsv.seconds(49));
        assertEquals("0,1", GroupCsv.seconds(50));
        assertEquals("12,3", GroupCsv.seconds(12345));
        assertEquals("60,0", GroupCsv.seconds(60_000));
        assertEquals("3600,0", GroupCsv.seconds(3_600_000));
        assertEquals("0,0", GroupCsv.seconds(-5));
    }

    @Test
    public void headerColumnsMatchRow() {
        GroupRun run = run("a");
        assertEquals(GroupCsv.HEADER.size(), GroupCsv.row(run.getItems().get(0)).size());
        assertEquals(Arrays.asList("Balík", "Cesta", "Typ", "Verze DMF", "Verdikt", "Chyby", "Varování", "Informace", "Doba (s)",
                "Protokol XML", "Textový log", "Chybová hláška"), GroupCsv.HEADER);
    }
}
