package io.github.datromtool.io.copy.archive.impl.process;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.io.ArchiveContentsDependantTest;
import io.github.datromtool.io.copy.archive.exceptions.ArchiveEntryNotFoundException;
import io.github.datromtool.util.ArchiveUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIf(
        value = "io.github.datromtool.util.ArchiveUtils#isSevenZipAvailable",
        disabledReason = "7-Zip is not available")
class SevenZipRarArchiveSourceSpecTest extends ArchiveContentsDependantTest {

    static Path rarFile;
    static Path sevenZipPath;

    @BeforeAll
    static void resolveFile() {
        rarFile = archiveTestDataSource.resolve("archives").resolve("files.rar");
        sevenZipPath = requireNonNull(ArchiveUtils.getSevenZipPath());
    }

    @Test
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