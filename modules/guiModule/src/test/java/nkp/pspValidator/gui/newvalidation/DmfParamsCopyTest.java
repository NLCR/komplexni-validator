package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.DmfDetector;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Kopie {@link DmfDetector.Params}, ze které žije neměnný {@link ValidationRequest}.
 */
public class DmfParamsCopyTest {

    @Test
    public void copiesEveryFieldIndependently() throws Exception {
        DmfDetector.Params original = new DmfDetector.Params();
        int fields = 0;
        for (Field field : DmfDetector.Params.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.set(original, "v-" + field.getName());
                fields++;
            }
        }
        //11 typu, vynucena a preferovana verze
        assertEquals(22, fields);

        DmfDetector.Params copy = DmfParamsBuilder.copy(original);
        assertNotSame(original, copy);
        for (Field field : DmfDetector.Params.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                assertEquals(field.getName(), "v-" + field.getName(), field.get(copy));
            }
        }
        original.forcedDmfFduVersion = "jina";
        assertEquals("v-forcedDmfFduVersion", copy.forcedDmfFduVersion);
    }

    @Test
    public void copiesNullValuesAndNull() {
        DmfDetector.Params original = new DmfDetector.Params();
        original.preferredDmfMonVersion = "1.4";
        DmfDetector.Params copy = DmfParamsBuilder.copy(original);
        assertEquals("1.4", copy.preferredDmfMonVersion);
        assertNull(copy.forcedDmfMonVersion);
        assertNull(DmfParamsBuilder.copy(null));
    }
}
