package nkp.pspValidator.gui.group;

import nkp.pspValidator.gui.newvalidation.DocumentTypes;
import nkp.pspValidator.gui.results.ProblemCounts;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Export souhrnu skupiny do CSV pro Excel: UTF-8 s BOM, oddělovač středník, řádky ukončené CRLF. Pole se středníkem,
 * uvozovkou nebo koncem řádku se uzavře do uvozovek a uvozovky uvnitř se zdvojí (RFC 4180). Doba je v sekundách
 * s desetinnou čárkou (jedno desetinné místo), aby ji česky nastavený Excel přečetl jako číslo. Hodnoty, které balík
 * nemá (nevalidoval se, chybí protokol), jsou prázdné. Bez JavaFX.
 */
public final class GroupCsv {

    public static final List<String> HEADER = Arrays.asList("Balík", "Cesta", "Typ", "Verze DMF", "Verdikt", "Chyby",
            "Varování", "Informace", "Doba (s)", "Protokol XML", "Textový log", "Chybová hláška");

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final char SEPARATOR = ';';
    private static final String LINE_END = "\r\n";

    private GroupCsv() {
    }

    /** Zapíše hlavičku a jeden řádek za každý balík (v daném pořadí). Proud nezavírá. */
    public static void write(List<GroupItem> items, OutputStream out) throws IOException {
        out.write(BOM);
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        writeRow(writer, HEADER);
        for (GroupItem item : items) {
            writeRow(writer, row(item));
        }
        writer.flush();
    }

    /** Hodnoty řádku balíku v pořadí {@link #HEADER}. */
    static List<String> row(GroupItem item) {
        ProblemCounts counts = item.getCounts();
        return Arrays.asList(
                item.getName(),
                item.getDisplayPath(),
                item.getDmf() == null ? "" : DocumentTypes.name(item.getDmf().getType()),
                item.getDmf() == null ? "" : item.getDmf().getVersion(),
                item.getStatus().getLabel(),
                counts == null ? "" : String.valueOf(counts.getErrors()),
                counts == null ? "" : String.valueOf(counts.getWarnings()),
                counts == null ? "" : String.valueOf(counts.getInfos()),
                item.getDurationMs() == null ? "" : seconds(item.getDurationMs()),
                path(item.getXmlProtocol()),
                path(item.getTxtLog()),
                item.getMessage() == null ? "" : item.getMessage());
    }

    /** Milisekundy jako sekundy s jedním desetinným místem a čárkou: 12345 → „12,3“, 50 → „0,1“. */
    static String seconds(long millis) {
        long tenths = Math.round(Math.max(0, millis) / 100.0);
        return (tenths / 10) + "," + (tenths % 10);
    }

    private static String path(File file) {
        return file == null ? "" : file.getAbsolutePath();
    }

    private static void writeRow(Writer writer, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(SEPARATOR);
            }
            writer.write(field(values.get(i)));
        }
        writer.write(LINE_END);
    }

    /** Pole CSV: v uvozovkách, když obsahuje středník, uvozovku nebo konec řádku; uvozovky uvnitř zdvojené. */
    static String field(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(SEPARATOR) < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
