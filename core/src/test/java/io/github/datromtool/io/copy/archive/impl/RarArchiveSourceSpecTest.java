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

class RarArchiveSourceSpecTest extends ArchiveContentsDependantTest {

    static Path rarFile;

    @BeforeAll
    static void resolveFile() {
        rarFile = archiveTestDataSource.resolve("archives").resolve("files.rar4.rar");
    }

    @Test
    void testReadContents() throws IOException {
        try (RarArchiveSourceSpec spec = new RarArchiveSourceSpec(rarFile)) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), true);
            assertIsShortText(spec.getNextInternalSpec(), true);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testAllContentsInOrder() throws IOException {
        try (RarArchiveSourceSpec spec = new RarArchiveSourceSpec(rarFile, ImmutableList.of(LOREM_IPSUM_FILE, SHORT_TEXT_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), true);
            assertIsShortText(spec.getNextInternalSpec(), true);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testAllContentsInReverse_shouldStillBePhysicalOrder() throws IOException {
        try (RarArchiveSourceSpec spec = new RarArchiveSourceSpec(rarFile, ImmutableList.of(SHORT_TEXT_FILE, LOREM_IPSUM_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), true);
            assertIsShortText(spec.getNextInternalSpec(), true);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadOnlyLoremIpsum() throws IOException {
        try (RarArchiveSourceSpec spec = new RarArchiveSourceSpec(rarFile, ImmutableList.of(LOREM_IPSUM_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), true);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadOnlyShortText() throws IOException {
        try (RarArchiveSourceSpec spec = new RarArchiveSourceSpec(rarFile, ImmutableList.of(SHORT_TEXT_FILE))) {
            assertIsShortText(spec.getNextInternalSpec(), true);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadShortTextAndThenUnknown() throws IOException {
        try (RarArchiveSourceSpec spec = new RarArchiveSourceSpec(rarFile, ImmutableList.of(SHORT_TEXT_FILE, "unknownFile"))) {
            assertIsShortText(spec.getNextInternalSpec(), true);
            assertThrows(ArchiveEntryNotFoundException.class, spec::getNextInternalSpec);
        }
    }

}