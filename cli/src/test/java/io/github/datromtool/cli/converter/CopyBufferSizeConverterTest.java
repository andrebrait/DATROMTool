package io.github.datromtool.cli.converter;

import io.github.datromtool.config.CopyBufferSize;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Direct unit tests for {@link CopyBufferSizeConverter} (issue #14 step 3). */
class CopyBufferSizeConverterTest {

    private static final CopyBufferSizeConverter CONVERTER = new CopyBufferSizeConverter();

    @Test
    void parsesPlainByteCount() {
        assertEquals(new CopyBufferSize(1024), CONVERTER.convert("1024"));
    }

    @Test
    void parsesKilobyteSuffix() {
        assertEquals(new CopyBufferSize(32 * 1024), CONVERTER.convert("32KB"));
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
