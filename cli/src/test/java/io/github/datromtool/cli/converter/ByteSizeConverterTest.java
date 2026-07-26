package io.github.datromtool.cli.converter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Row 29: direct unit tests for {@link ByteSizeConverter}. */
class ByteSizeConverterTest {

    private static final ByteSizeConverter CONVERTER = new ByteSizeConverter();

    @Test
    void parsesPlainByteCount() {
        assertEquals(1024L, CONVERTER.convert("1024").getSizeInBytes(), "'1024' must parse as 1024 bytes");
    }

    @Test
    void parsesKilobyteSuffix() {
        assertEquals(32 * 1024L, CONVERTER.convert("32KB").getSizeInBytes(), "'32KB' must parse as 32768 bytes");
    }

    @Test
    void parsesMegabyteSuffix() {
        assertEquals(1024 * 1024L, CONVERTER.convert("1MB").getSizeInBytes(), "'1MB' must parse as 1048576 bytes");
    }

    @Test
    void garbageValueFailsToParse() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CONVERTER.convert("abc"),
                "a non-numeric value must fail to parse as a ByteSize");
    }
}
