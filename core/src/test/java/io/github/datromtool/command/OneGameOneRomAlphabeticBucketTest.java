package io.github.datromtool.command;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junitpioneer.jupiter.DefaultLocale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which subfolder an alphabetically-organised output run files a game under must depend on the
 * game's name, not on the machine's locale: folding with the default locale files "Ikari
 * Warriors" under "#" in Turkish, because 'I' lowers to the dotless 'i' there.
 */
@DefaultLocale("tr-TR")
class OneGameOneRomAlphabeticBucketTest {

    @ParameterizedTest
    @CsvSource({
            "Ikari Warriors (USA), i",
            "Adventure (USA), a",
            "3-D Tic-Tac-Toe (USA), #"})
    void alphabeticOutputFolderIsChosenRegardlessOfLocale(String gameName, String expectedBucket) {
        assertEquals(
                expectedBucket,
                OneGameOneRom.alphabeticBucket(gameName),
                "the alphabetic output subfolder must not depend on the machine's locale");
    }
}
