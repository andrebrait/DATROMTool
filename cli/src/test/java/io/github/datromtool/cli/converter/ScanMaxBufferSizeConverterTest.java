package io.github.datromtool.cli.converter;

import io.github.datromtool.config.ScanMaxBufferSize;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Direct unit tests for {@link ScanMaxBufferSizeConverter} (issue #14 step 3). */
class ScanMaxBufferSizeConverterTest {

    private static final ScanMaxBufferSizeConverter CONVERTER = new ScanMaxBufferSizeConverter();

    @Test
    void parsesPlainByteCount() {
        assertEquals(new ScanMaxBufferSize(1024), CONVERTER.convert("1024"));
    }

    @Test
    void parsesMegabyteSuffix() {
        assertEquals(new ScanMaxBufferSize(256 * 1024 * 1024), CONVERTER.convert("256MB"));
    }

    @Test
    void overIntegerMaxValueFailsWithPreservedMessage() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> CONVERTER.convert("3GB"));
        assertEquals(format("Maximum byte size is %d bytes", Integer.MAX_VALUE), thrown.getMessage());
    }

    @Test
    void zeroFailsValidation() {
        assertThrows(IllegalArgumentException.class, () -> CONVERTER.convert("0"));
    }
}
