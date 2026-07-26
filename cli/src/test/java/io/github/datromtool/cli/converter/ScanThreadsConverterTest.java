package io.github.datromtool.cli.converter;

import io.github.datromtool.config.ScanThreads;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Direct unit tests for {@link ScanThreadsConverter} (issue #14 step 3). */
class ScanThreadsConverterTest {

    private static final ScanThreadsConverter CONVERTER = new ScanThreadsConverter();

    @Test
    void parsesPositiveInt() {
        assertEquals(new ScanThreads(4), CONVERTER.convert("4"));
    }

    @Test
    void zeroFailsValidationWithPositiveMessage() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> CONVERTER.convert("0"));
        assertTrue(thrown.getMessage().toLowerCase().contains("positive"));
    }

    @Test
    void negativeFailsValidation() {
        assertThrows(IllegalArgumentException.class, () -> CONVERTER.convert("-1"));
    }

    @Test
    void garbageValueFailsToParse() {
        assertThrows(NumberFormatException.class, () -> CONVERTER.convert("abc"));
    }
}
