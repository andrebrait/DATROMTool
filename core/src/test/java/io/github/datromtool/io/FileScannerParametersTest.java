package io.github.datromtool.io;

import org.junit.jupiter.api.Test;

import static io.github.datromtool.io.FileScannerParameters.DEFAULT_BUFFER_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileScannerParametersTest {

    @Test
    void testWithDefaults() {
        FileScannerParameters defaults = FileScannerParameters.withDefaults();
        assertNotNull(defaults);
        assertTrue(defaults.getAlsoScanArchives().isEmpty());
        assertEquals(defaults.getBufferSize(), DEFAULT_BUFFER_SIZE);
        assertEquals(defaults.getMinRomSize(), 0L);
        assertEquals(defaults.getMaxRomSize(), Long.MAX_VALUE);
        assertEquals(defaults.getMinRomSizeStr(), "0.00 B");
        assertEquals(defaults.getMaxRomSizeStr(), "8.00 EB");
        assertFalse(defaults.isUseLazyDetector());
    }

    @Test
    void testForDatWithDetector() {
    }
}