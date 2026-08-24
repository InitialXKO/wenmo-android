package ink.wenmo.ime.engine;

import org.junit.Test;
import static org.junit.Assert.*;

public class RustInputEngineTest {

    @Test
    public void testEngineBasicLifecycle() {
        RustInputEngine engine = new RustInputEngine(null);

        assertNotNull(engine.composition());
        assertNotNull(engine.candidates());

        engine.type('w');
        engine.type('e');
        engine.type('n');
        engine.type('m');
        engine.type('o');

        assertFalse(engine.composition().isEmpty());

        engine.setTraditional(true);
        assertTrue(engine.isTraditional());

        engine.setTraditional(false);
        assertFalse(engine.isTraditional());

        engine.backspace();
        assertEquals("wenm", engine.composition());

        engine.clear();
        assertEquals("", engine.composition());
        assertTrue(engine.candidates().isEmpty());
    }
}
