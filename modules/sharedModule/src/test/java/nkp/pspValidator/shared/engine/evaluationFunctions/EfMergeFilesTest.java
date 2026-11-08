package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Funkce mergeFiles: slouceni parametru file, file_list a file_list_list (vsechny volitelne, libovolny pocet).
 */
public class EfMergeFilesTest {

    private static final String FUNCTION_NAME = "mergeFiles";

    private final File a = new File("a.xml");
    private final File b = new File("b.xml");
    private final File c = new File("c.xml");

    private Engine engine;

    @Before
    public void setup() {
        engine = new Engine(null);
    }

    private EvaluationFunction fn() {
        return engine.buildEvaluationFunction(FUNCTION_NAME);
    }

    @SuppressWarnings("unchecked")
    private List<File> data(ValueEvaluation evaluation) {
        assertNull(evaluation.getErrorMessage(), evaluation.getErrorMessage());
        return (List<File>) evaluation.getData();
    }

    @Test
    public void twoNonEmptyLists() {
        ValueEvaluation ev = fn()
                .withValueParam("file_list", ValueType.FILE_LIST, new ValueEvaluation(Arrays.asList(a, b)))
                .withValueParam("file_list", ValueType.FILE_LIST, new ValueEvaluation(Collections.singletonList(c)))
                .evaluate();
        assertEquals(Arrays.asList(a, b, c), data(ev));
    }

    @Test
    public void emptyListIsIgnored() {
        ValueEvaluation ev = fn()
                .withValueParam("file_list", ValueType.FILE_LIST, new ValueEvaluation(Collections.<File>emptyList()))
                .withValueParam("file_list", ValueType.FILE_LIST, new ValueEvaluation(Collections.singletonList(a)))
                .evaluate();
        assertEquals(Collections.singletonList(a), data(ev));
    }

    @Test
    public void onlyEmptyListGivesEmptyList() {
        ValueEvaluation ev = fn()
                .withValueParam("file_list", ValueType.FILE_LIST, new ValueEvaluation(Collections.<File>emptyList()))
                .evaluate();
        assertTrue(data(ev).isEmpty());
    }

    @Test
    public void noParamsGivesEmptyList() {
        assertTrue(data(fn().evaluate()).isEmpty());
    }

    @Test
    public void duplicateFileIsNotDeduplicated() {
        ValueEvaluation ev = fn()
                .withValueParam("file_list", ValueType.FILE_LIST, new ValueEvaluation(Arrays.asList(a, b)))
                .withValueParam("file_list", ValueType.FILE_LIST, new ValueEvaluation(Arrays.asList(b, a)))
                .evaluate();
        assertEquals(Arrays.asList(a, b, b, a), data(ev));
    }

    @Test
    public void singleFilesAndListsAndListLists() {
        ValueEvaluation ev = fn()
                .withValueParam("file_list_list", ValueType.FILE_LIST_LIST,
                        new ValueEvaluation(Arrays.asList(Collections.singletonList(c), Collections.singletonList(a))))
                .withValueParam("file_list", ValueType.FILE_LIST, new ValueEvaluation(Collections.singletonList(b)))
                .withValueParam("file", ValueType.FILE, new ValueEvaluation(a))
                .evaluate();
        //poradi: nejdriv jednotlive soubory, pak seznamy, pak seznamy seznamu (bez ohledu na poradi pridani parametru)
        assertEquals(Arrays.asList(a, b, c, a), data(ev));
    }

    @Test
    public void nullFileListParamGivesErrorEvaluationNotException() {
        ValueEvaluation ev = fn()
                .withValueParam("file_list", ValueType.FILE_LIST, new ValueEvaluation(null, "neznama hodnota"))
                .evaluate();
        assertNull(ev.getData());
        assertNotNull(ev.getErrorMessage());
        assertTrue(ev.getErrorMessage(), ev.getErrorMessage().contains("file_list"));
        assertTrue(ev.getErrorMessage(), ev.getErrorMessage().contains("neznama hodnota"));
    }

    @Test
    public void nullFileParamGivesErrorEvaluation() {
        ValueEvaluation ev = fn()
                .withValueParam("file", ValueType.FILE, new ValueEvaluation(null, "chyba"))
                .evaluate();
        assertNull(ev.getData());
        assertTrue(ev.getErrorMessage(), ev.getErrorMessage().contains("file"));
    }

    @Test
    public void nullFileListListParamGivesErrorEvaluation() {
        ValueEvaluation ev = fn()
                .withValueParam("file_list_list", ValueType.FILE_LIST_LIST, new ValueEvaluation(null, "chyba"))
                .evaluate();
        assertNull(ev.getData());
        assertTrue(ev.getErrorMessage(), ev.getErrorMessage().contains("file_list_list"));
    }

    @Test
    public void nullListInsideListListGivesUnexpectedError() {
        //znama vada: null seznam uvnitr file_list_list neni osetren, skonci jako "necekana chyba" (NPE) misto chyby parametru
        ValueEvaluation ev = fn()
                .withValueParam("file_list_list", ValueType.FILE_LIST_LIST,
                        new ValueEvaluation(Collections.singletonList((List<File>) null)))
                .evaluate();
        assertNull(ev.getData());
        assertNotNull(ev.getErrorMessage());
    }

    @Test
    public void unknownParamViolatesContract() {
        ValueEvaluation ev = fn()
                .withValueParam("neznamy", ValueType.FILE, new ValueEvaluation(a))
                .evaluate();
        assertNull(ev.getData());
        assertNotNull(ev.getErrorMessage());
    }
}
