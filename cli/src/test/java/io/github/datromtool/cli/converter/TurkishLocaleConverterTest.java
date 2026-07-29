package io.github.datromtool.cli.converter;

import io.github.datromtool.data.OutputMode;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Option values are case-folded before they reach filtering and sorting, so the fold must not
 * depend on the machine's locale: under Turkish rules {@code "it".toUpperCase()} is {@code "İT"}
 * and {@code "IT".toLowerCase()} is {@code "ıt"}, neither of which matches any region or
 * language the rest of the pipeline knows.
 */
@DefaultLocale("tr-TR")
class TurkishLocaleConverterTest {

    @Test
    void regionValuesUpperCaseRegardlessOfLocale() {
        assertEquals(
                "ITA",
                new TrimmingUpperCaseConverter().convert(" ita "),
                "a region value must upper-case to ASCII under any locale");
    }

    @Test
    void languageValuesLowerCaseRegardlessOfLocale() {
        assertEquals(
                "it",
                new TrimmingLowerCaseConverter().convert(" IT "),
                "a language value must lower-case to ASCII under any locale");
    }

    @Test
    void outputModeValuesConvertRegardlessOfLocale() {
        assertEquals(
                OutputMode.JSON,
                new OutputModeConverter().convert("JSON"),
                "an output mode must convert under any locale");
    }
}
