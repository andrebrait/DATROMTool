package io.github.datromtool.io.copy.archive.impl.process;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.io.ArchiveContentsDependantTest;
import io.github.datromtool.io.copy.archive.exceptions.ArchiveEntryNotFoundException;
import io.github.datromtool.util.ArchiveUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junitpioneer.jupiter.DefaultTimeZone;

import java.io.IOException;
import java.nio.file.Path;
import java.util.TimeZone;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@EnabledIf(
        value = "io.github.datromtool.util.ArchiveUtils#isSevenZipAvailable",
        disabledReason = "7-Zip is not available")
class SevenZipRarArchiveSourceSpecTest extends ArchiveContentsDependantTest {

    static Path rarFile;
    static Path sevenZipPath;

    @BeforeAll
    static void resolveFile() {
        // Load class and cache time zone information
        log.info("Starting tests using '{}'", SevenZipRarArchiveSourceSpec.class.getName());
        rarFile = archiveTestDataSource.resolve("archives").resolve("files.rar");
        sevenZipPath = requireNonNull(ArchiveUtils.getSevenZipPath());
    }
    @Test
    @DefaultTimeZone("America/Los_Angeles")
    void testReadContents_LosAngeles() throws IOException {
        assertEquals(TimeZone.getTimeZone("America/Los_Angeles"), TimeZone.getDefault());
        testReadContents();
    }

    @Test
    @DefaultTimeZone("America/Sao_Paulo")
    void testReadContents_SaoPaulo() throws IOException {
        assertEquals(TimeZone.getTimeZone("America/Sao_Paulo"), TimeZone.getDefault());
        testReadContents();
    }

    @Test
    @DefaultTimeZone("Europe/Amsterdam")
    void testReadContents_Amsterdam() throws IOException {
        assertEquals(TimeZone.getTimeZone("Europe/Amsterdam"), TimeZone.getDefault());
        testReadContents();
    }

    @Test
    @DefaultTimeZone("Asia/Kolkata")
    void testReadContents_Kolkata() throws IOException {
        assertEquals(TimeZone.getTimeZone("Asia/Kolkata"), TimeZone.getDefault());
        testReadContents();
    }

    void testReadContents() throws IOException {
        try (SevenZipRarArchiveSourceSpec spec = new SevenZipRarArchiveSourceSpec(sevenZipPath, rarFile)) {
            assertIsLoremIpsum(spec.getNextInternalSpec());
            assertIsShortText(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testAllContentsInOrder() throws IOException {
        try (SevenZipRarArchiveSourceSpec spec = new SevenZipRarArchiveSourceSpec(sevenZipPath, rarFile, ImmutableList.of(SHORT_TEXT_FILE, LOREM_IPSUM_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec());
            assertIsShortText(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testAllContentsInReverse_shouldStillBePhysicalOrder() throws IOException {
        try (SevenZipRarArchiveSourceSpec spec = new SevenZipRarArchiveSourceSpec(sevenZipPath, rarFile, ImmutableList.of(LOREM_IPSUM_FILE, SHORT_TEXT_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec());
            assertIsShortText(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadOnlyLoremIpsum() throws IOException {
        try (SevenZipRarArchiveSourceSpec spec = new SevenZipRarArchiveSourceSpec(sevenZipPath, rarFile, ImmutableList.of(LOREM_IPSUM_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadOnlyShortText() throws IOException {
        try (SevenZipRarArchiveSourceSpec spec = new SevenZipRarArchiveSourceSpec(sevenZipPath, rarFile, ImmutableList.of(SHORT_TEXT_FILE))) {
            assertIsShortText(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadShortTextAndThenUnknown() throws IOException {
        try (SevenZipRarArchiveSourceSpec spec = new SevenZipRarArchiveSourceSpec(sevenZipPath, rarFile, ImmutableList.of(SHORT_TEXT_FILE, "unknownFile"))) {
            assertThrows(ArchiveEntryNotFoundException.class, spec::getNextInternalSpec);
        }
    }

}