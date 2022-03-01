package io.github.datromtool.io.copy.archive.impl;

import io.github.datromtool.io.ArchiveContentsDependantTest;
import io.github.datromtool.io.compression.CompressionAlgorithm;
import io.github.datromtool.io.copy.SourceSpec;
import io.github.datromtool.io.copy.archive.ArchiveDestinationInternalSpec;
import io.github.datromtool.io.copy.archive.ArchiveDestinationSpec;
import io.github.datromtool.io.copy.impl.FileSourceSpec;
import io.github.datromtool.util.ArchiveUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
class TarArchiveDestinationSpecTest extends ArchiveContentsDependantTest {

    private Path tempDir;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("datromtool_tarArchive_test_");
    }

    @AfterEach
    void tearDown() throws Exception {
        ArchiveUtils.deleteFolder(tempDir);
    }

    static Stream<Arguments> algorithms() {
        return Arrays.stream(CompressionAlgorithm.values())
                .filter(a -> {
                    if (a.isEnabled()) {
                        return true;
                    } else {
                        log.warn("{} is disabled. Will not run compression tests for {}.", a, a);
                        return false;
                    }
                }).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void testWriteFileToSevenZip(CompressionAlgorithm algorithm) throws IOException {
        Path file = tempDir.resolve("testWriteFileToSevenZip.tar" + (algorithm != null ? "." + algorithm.getExtension() : null));
        try (ArchiveDestinationSpec destinationSpec = new TarArchiveDestinationSpec(algorithm, file)) {
            writeFile(SHORT_TEXT_FILE, destinationSpec);
            writeFile(LOREM_IPSUM_FILE, destinationSpec);
        }
        try (TarArchiveSourceSpec spec = new TarArchiveSourceSpec(algorithm, file)) {
            assertIsLocalShortText(spec.getNextInternalSpec(), true, false);
            assertIsLocalLoremIpsum(spec.getNextInternalSpec(), true, false);
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
