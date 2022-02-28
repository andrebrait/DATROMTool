package io.github.datromtool.io.spec.implementations;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.io.ArchiveContentsDependantTest;
import io.github.datromtool.io.spec.exceptions.ArchiveEntryNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZipArchiveSourceSpecTest extends ArchiveContentsDependantTest {

    static Path zipFile;

    @BeforeAll
    static void resolveFile() {
        zipFile = archiveTestDataSource.resolve("files.zip");
    }

    @Test
    void testReadContents() throws IOException {
        try (ZipArchiveSourceSpec spec = new ZipArchiveSourceSpec(zipFile)) {
            assertIsLoremIpsum(spec.getNextInternalSpec());
            assertIsShortText(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testAllContentsInOrder() throws IOException {
        try (ZipArchiveSourceSpec spec = new ZipArchiveSourceSpec(zipFile, ImmutableList.of(LOREM_IPSUM_FILE, SHORT_TEXT_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec());
            assertIsShortText(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testAllContentsInReverse_shouldStillBePhysicalOrder() throws IOException {
        try (ZipArchiveSourceSpec spec = new ZipArchiveSourceSpec(zipFile, ImmutableList.of(SHORT_TEXT_FILE, LOREM_IPSUM_FILE))) {
            // Order should still be the physical order in the file
            assertIsLoremIpsum(spec.getNextInternalSpec());
            assertIsShortText(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadOnlyLoremIpsum() throws IOException {
        try (ZipArchiveSourceSpec spec = new ZipArchiveSourceSpec(zipFile, ImmutableList.of(LOREM_IPSUM_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadOnlyShortText() throws IOException {
        try (ZipArchiveSourceSpec spec = new ZipArchiveSourceSpec(zipFile, ImmutableList.of(SHORT_TEXT_FILE))) {
            assertIsShortText(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadShortTextAndThenUnknown() throws IOException {
        try (ZipArchiveSourceSpec spec = new ZipArchiveSourceSpec(zipFile, ImmutableList.of(SHORT_TEXT_FILE, "unknownFile"))) {
            assertIsShortText(spec.getNextInternalSpec());
            assertThrows(ArchiveEntryNotFoundException.class, spec::getNextInternalSpec);
        }
    }

}