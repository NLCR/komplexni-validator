package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.types.Identifier;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class EfFilterIdentifersByTypesTest {

    private static final String FUNCTION_NAME = "filterIdentifersByTypes"; //sic: v kodu i ve fDMF je preklep "Identifers";

    private final Identifier uuid1 = new Identifier("uuid", "1");
    private final Identifier uuid2 = new Identifier("uuid", "2");
    private final Identifier urn = new Identifier("urnnbn", "urn:nbn:cz:abc-000001");
    private final Identifier isbn = new Identifier("isbn", "978");

    private Engine engine;

    @Before
    public void setup() {
        engine = new Engine(null);
    }

    private ValueEvaluation filter(List<Identifier> ids, List<String> types) {
        return engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam("identifiers", ValueType.IDENTIFIER_LIST, new ValueEvaluation(ids))
                .withValueParam("types", ValueType.STRING_LIST, new ValueEvaluation(types))
                .evaluate();
    }

    @SuppressWarnings("unchecked")
    private List<Identifier> ok(ValueEvaluation ev) {
        assertNull(ev.getErrorMessage(), ev.getErrorMessage());
        return (List<Identifier>) ev.getData();
    }

    @Test
    public void matchingTypesKeepOrder() {
        List<Identifier> result = ok(filter(Arrays.asList(uuid1, urn, isbn, uuid2), Arrays.asList("uuid", "isbn")));
        assertEquals(Arrays.asList(uuid1, isbn, uuid2), result);
    }

    @Test
    public void noMatch() {
        assertTrue(ok(filter(Arrays.asList(uuid1, urn), Collections.singletonList("sysno"))).isEmpty());
    }

    @Test
    public void emptyIdentifiers() {
        assertTrue(ok(filter(Collections.<Identifier>emptyList(), Collections.singletonList("uuid"))).isEmpty());
    }

    @Test
    public void emptyTypes() {
        assertTrue(ok(filter(Arrays.asList(uuid1, urn), Collections.<String>emptyList())).isEmpty());
    }

    @Test
    public void typesAreCaseSensitive() {
        assertTrue(ok(filter(Arrays.asList(uuid1, urn), Arrays.asList("UUID", "urnNBN"))).isEmpty());
    }

    @Test
    public void identifierWithNullTypeIsNotMatched() {
        assertTrue(ok(filter(Collections.singletonList(new Identifier(null, "x")), Collections.singletonList("uuid"))).isEmpty());
    }

    @Test
    public void nullIdentifiersParamGivesErrorEvaluation() {
        ValueEvaluation ev = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam("identifiers", ValueType.IDENTIFIER_LIST, new ValueEvaluation(null, "chyba"))
                .withValueParam("types", ValueType.STRING_LIST, new ValueEvaluation(Collections.singletonList("uuid")))
                .evaluate();
        assertNull(ev.getData());
        assertTrue(ev.getErrorMessage(), ev.getErrorMessage().contains("identifiers"));
    }

    @Test
    public void nullTypesParamGivesErrorEvaluation() {
        ValueEvaluation ev = filter(Collections.singletonList(uuid1), null);
        assertNull(ev.getData());
        assertTrue(ev.getErrorMessage(), ev.getErrorMessage().contains("types"));
    }

    @Test
    public void missingParamViolatesContract() {
        ValueEvaluation ev = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam("identifiers", ValueType.IDENTIFIER_LIST, new ValueEvaluation(Collections.singletonList(uuid1)))
                .evaluate();
        assertNull(ev.getData());
        assertNotNull(ev.getErrorMessage());
    }
}
