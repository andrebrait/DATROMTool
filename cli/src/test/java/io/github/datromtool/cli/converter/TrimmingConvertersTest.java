package io.github.datromtool.cli.converter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Row 28: direct unit tests for the trim + case-fold converters used by region/language options. */
class TrimmingConvertersTest {

    @Test
    void upperCaseConverterTrimsAndUppercases() {
        assertEquals(
                "AB",
                new TrimmingUpperCaseConverter().convert(" aB "),
                "TrimmingUpperCaseConverter must trim surrounding whitespace and uppercase");
    }

    @Test
    void lowerCaseConverterTrimsAndLowercases() {
        assertEquals(
                "ab",
                new TrimmingLowerCaseConverter().convert(" aB "),
                "TrimmingLowerCaseConverter must trim surrounding whitespace and lowercase");
    }
}
