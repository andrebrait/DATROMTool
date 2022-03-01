package io.github.datromtool.io.copy.impl;

import io.github.datromtool.io.ArchiveContentsDependantTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileSourceSpecTest extends ArchiveContentsDependantTest {

    @Test
    void testReadLoremIpsum() throws IOException {
        try (FileSourceSpec spec = FileSourceSpec.from(archiveTestDataSource.resolve(LOREM_IPSUM_FILE))) {
            assertIsLoremIpsumContents(spec);
            assertEquals(loremIpsumLocalTimes, spec.getFileTimes());
        }
    }

    @Test
    void testReadShortText() throws IOException {
        try (FileSourceSpec spec = FileSourceSpec.from(archiveTestDataSource.resolve(SHORT_TEXT_FILE))) {
            assertIsShortTextContents(spec);
            assertEquals(shortTextLocalTimes, spec.getFileTimes());
        }
    }
}