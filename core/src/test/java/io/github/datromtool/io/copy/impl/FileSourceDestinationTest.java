package io.github.datromtool.io.copy.impl;

import io.github.datromtool.io.ArchiveContentsDependantTest;
import io.github.datromtool.io.copy.DestinationSpec;
import io.github.datromtool.io.copy.SourceSpec;
import io.github.datromtool.util.ArchiveUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileSourceDestinationTest extends ArchiveContentsDependantTest {

    private Path tempDir;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("datromtool_fileCopySpec_test_");
    }

    @AfterEach
    void tearDown() throws Exception {
        ArchiveUtils.deleteFolder(tempDir);
    }

    @Test
    void testWriteLoremIpsum() throws IOException {
        Path tempFile = tempDir.resolve("lorem-ipsum.txt");
        writeFile(LOREM_IPSUM_FILE, tempFile);
        try (FileSourceSpec spec = FileSourceSpec.from(tempFile)) {
            assertIsLoremIpsumContents(spec);
            assertEquals(loremIpsumLocalTimes, spec.getFileTimes());
        }
    }

    @Test
    void testWriteShortText() throws IOException {
        Path tempFile = tempDir.resolve("short-text.txt");
        writeFile(SHORT_TEXT_FILE, tempFile);
        try (FileSourceSpec spec = FileSourceSpec.from(tempFile)) {
            assertIsShortTextContents(spec);
            assertEquals(shortTextLocalTimes, spec.getFileTimes());
        }
    }

    private static void writeFile(String filePath, Path destination) throws IOException {
        try (SourceSpec sourceSpec = FileSourceSpec.from(archiveTestDataSource.resolve(filePath))) {
            try (DestinationSpec destinationSpec = FileDestinationSpec.of(destination, sourceSpec)) {
                IOUtils.copy(sourceSpec.getInputStream(), destinationSpec.getOutputStream());
            }
        }
    }
}