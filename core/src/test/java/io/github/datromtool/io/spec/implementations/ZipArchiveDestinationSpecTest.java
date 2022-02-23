package io.github.datromtool.io.spec.implementations;

import io.github.datromtool.io.ArchiveContentsDependantTest;
import io.github.datromtool.io.spec.ArchiveDestinationInternalSpec;
import io.github.datromtool.io.spec.ArchiveDestinationSpec;
import io.github.datromtool.io.spec.SourceSpec;
import io.github.datromtool.util.ArchiveUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;

class ZipArchiveDestinationSpecTest extends ArchiveContentsDependantTest {

    private Path tempDir;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("datromtool_zipArchive_test_");
    }

    @AfterEach
    void tearDown() throws Exception {
        ArchiveUtils.deleteFolder(tempDir);
    }

    @Test
    void testWriteFileToZip() throws IOException {
        Path file = tempDir.resolve("testWriteFileToZip.zip");
        try (ArchiveDestinationSpec destinationSpec = ZipArchiveDestinationSpec.of(file)) {
            writeFile(SHORT_TEXT_FILE, destinationSpec);
            writeFile(LOREM_IPSUM_FILE, destinationSpec);
        }
        try (ZipArchiveSourceSpec spec = ZipArchiveSourceSpec.from(file)) {
            assertIsLocalShortText(spec.getNextInternalSpec());
            assertIsLocalLoremIpsum(spec.getNextInternalSpec());
            assertNull(spec.getNextInternalSpec());
        }
    }

    private static void writeFile(String filePath, ArchiveDestinationSpec destinationSpec) throws IOException {
        try (SourceSpec sourceSpec = FileSourceSpec.from(archiveTestDataSource.resolve(filePath))) {
            try (ArchiveDestinationInternalSpec destinationInternalSpec = destinationSpec.createInternalDestinationSpecFor(filePath, sourceSpec)) {
                IOUtils.copy(sourceSpec.getInputStream(), destinationInternalSpec.getOutputStream());
            }
        }
    }
}
