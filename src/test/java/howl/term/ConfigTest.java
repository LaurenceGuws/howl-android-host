package howl.term;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class ConfigTest {
    @Test
    public void normalizeReturnsNullForNullAndBlankInput() {
        assertNull(Config.normalize(null));
        assertNull(Config.normalize(""));
        assertNull(Config.normalize("   \t\n  "));
    }

    @Test
    public void normalizeTrimsAndPreservesMeaningfulValues() {
        assertEquals("/system/bin/sh", Config.normalize("  /system/bin/sh  "));
        assertEquals("echo hello", Config.normalize("echo hello"));
    }
}
