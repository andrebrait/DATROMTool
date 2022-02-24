package io.github.datromtool.io.spec.implementations;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.io.ArchiveContentsDependantTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;

class RarArchiveSourceSpecTest extends ArchiveContentsDependantTest {

    static Path rarFile;

    @BeforeAll
    static void resolveFile() {
        rarFile = archiveTestDataSource.resolve("files.rar4.rar");
    }

    @Test
    void testReadContents() throws IOException {
        try (RarArchiveSourceSpec spec = RarArchiveSourceSpec.from(rarFile)) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), true, true, true);
            assertIsShortText(spec.getNextInternalSpec(), true, true, true);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testAllContentsInOrder() throws IOException {
        try (RarArchiveSourceSpec spec = RarArchiveSourceSpec.from(rarFile, ImmutableList.of(LOREM_IPSUM_FILE, SHORT_TEXT_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), true, true, true);
            assertIsShortText(spec.getNextInternalSpec(), true, true, true);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testAllContentsInReverse() throws IOException {
        try (RarArchiveSourceSpec spec = RarArchiveSourceSpec.from(rarFile, ImmutableList.of(SHORT_TEXT_FILE, LOREM_IPSUM_FILE))) {
            assertIsShortText(spec.getNextInternalSpec(), true, true, true);
            assertIsLoremIpsum(spec.getNextInternalSpec(), true, true, true);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadOnlyLoremIpsum() throws IOException {
        try (RarArchiveSourceSpec spec = RarArchiveSourceSpec.from(rarFile, ImmutableList.of(LOREM_IPSUM_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), true, true, true);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @Test
    void testReadOnlyShortText() throws IOException {
        try (RarArchiveSourceSpec spec = RarArchiveSourceSpec.from(rarFile, ImmutableList.of(SHORT_TEXT_FILE))) {
            assertIsShortText(spec.getNextInternalSpec(), true, true, true);
            assertNull(spec.getNextInternalSpec());
        }
    }

}