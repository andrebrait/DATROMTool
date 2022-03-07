package io.github.datromtool.io.copy.archive.impl;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.io.ArchiveContentsDependantTest;
import io.github.datromtool.io.copy.archive.exceptions.ArchiveEntryNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SevenZipArchiveSourceSpecTest extends ArchiveContentsDependantTest {

    static Path sevenZipFile;

    @BeforeAll
    static void resolveFile() {
        sevenZipFile = archiveTestDataSource.resolve("archives").resolve("files.7z");
    }

    /*
    Test files for 7z only contain modification dates, so we are testing only against those
     */

    @Test
    void testReadContents() throws IOException {
        try (SevenZipArchiveSourceSpec spec = new SevenZipArchiveSourceSpec(sevenZipFile)) {
            assertIsLoremIpsum(spec.getNextInternalSpec());
            assertIsShortText(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testAllContentsInOrder() throws IOException {
        try (SevenZipArchiveSourceSpec spec = new SevenZipArchiveSourceSpec(sevenZipFile, ImmutableList.of(LOREM_IPSUM_FILE, SHORT_TEXT_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec());
            assertIsShortText(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testAllContentsInReverse_shouldStillBePhysicalOrder() throws IOException {
        try (SevenZipArchiveSourceSpec spec = new SevenZipArchiveSourceSpec(sevenZipFile, ImmutableList.of(SHORT_TEXT_FILE, LOREM_IPSUM_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec());
            assertIsShortText(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadOnlyLoremIpsum() throws IOException {
        try (SevenZipArchiveSourceSpec spec = new SevenZipArchiveSourceSpec(sevenZipFile, ImmutableList.of(LOREM_IPSUM_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadOnlyShortText() throws IOException {
        try (SevenZipArchiveSourceSpec spec = new SevenZipArchiveSourceSpec(sevenZipFile, ImmutableList.of(SHORT_TEXT_FILE))) {
            assertIsShortText(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadShortTextAndThenUnknown() throws IOException {
        try (SevenZipArchiveSourceSpec spec = new SevenZipArchiveSourceSpec(sevenZipFile, ImmutableList.of(SHORT_TEXT_FILE, "unknownFile"))) {
            assertIsShortText(spec.getNextInternalSpec());
            assertThrows(ArchiveEntryNotFoundException.class, spec::getNextInternalSpec);
        }
    }

}