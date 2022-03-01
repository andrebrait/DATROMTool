package io.github.datromtool.io.spec.implementations;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.io.ArchiveContentsDependantTest;
import io.github.datromtool.io.spec.compression.CompressionAlgorithm;
import io.github.datromtool.io.spec.exceptions.ArchiveEntryNotFoundException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TarArchiveSourceSpecTest extends ArchiveContentsDependantTest {

    static Stream<Arguments> tarArchives() {
        return Stream.of(
                Arguments.of(archiveTestDataSource.resolve("files.tar"), null),
                Arguments.of(archiveTestDataSource.resolve("files.tar.bz2"), CompressionAlgorithm.BZIP2),
                Arguments.of(archiveTestDataSource.resolve("files.tar.gz"), CompressionAlgorithm.GZIP),
                Arguments.of(archiveTestDataSource.resolve("files.tar.lz4"), CompressionAlgorithm.LZ4),
                Arguments.of(archiveTestDataSource.resolve("files.tar.lzma"), CompressionAlgorithm.LZMA),
                Arguments.of(archiveTestDataSource.resolve("files.tar.xz"), CompressionAlgorithm.XZ)
        );
    }

    /*
    Test files for 7z only contain modification dates, so we are testing only against those
     */

    @ParameterizedTest
    @MethodSource("tarArchives")
    void testReadContents(Path file, CompressionAlgorithm algorithm) throws IOException {
        try (TarArchiveSourceSpec spec = new TarArchiveSourceSpec(algorithm, file)) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), true, false, false);
            assertIsShortText(spec.getNextInternalSpec(), true, false, false);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @ParameterizedTest
    @MethodSource("tarArchives")
    void testAllContentsInOrder(Path file, CompressionAlgorithm algorithm) throws IOException {
        try (TarArchiveSourceSpec spec = new TarArchiveSourceSpec(algorithm, file, ImmutableList.of(LOREM_IPSUM_FILE, SHORT_TEXT_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), true, false, false);
            assertIsShortText(spec.getNextInternalSpec(), true, false, false);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @ParameterizedTest
    @MethodSource("tarArchives")
    void testAllContentsInReverse_shouldStillBePhysicalOrder(Path file, CompressionAlgorithm algorithm) throws IOException {
        try (TarArchiveSourceSpec spec = new TarArchiveSourceSpec(algorithm, file, ImmutableList.of(SHORT_TEXT_FILE, LOREM_IPSUM_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), true, false, false);
            assertIsShortText(spec.getNextInternalSpec(), true, false, false);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @ParameterizedTest
    @MethodSource("tarArchives")
    void testReadOnlyLoremIpsum(Path file, CompressionAlgorithm algorithm) throws IOException {
        try (TarArchiveSourceSpec spec = new TarArchiveSourceSpec(algorithm, file, ImmutableList.of(LOREM_IPSUM_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), true, false, false);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @ParameterizedTest
    @MethodSource("tarArchives")
    void testReadOnlyShortText(Path file, CompressionAlgorithm algorithm) throws IOException {
        try (TarArchiveSourceSpec spec = new TarArchiveSourceSpec(algorithm, file, ImmutableList.of(SHORT_TEXT_FILE))) {
            assertIsShortText(spec.getNextInternalSpec(), true, false, false);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @ParameterizedTest
    @MethodSource("tarArchives")
    void testReadShortTextAndThenUnknown(Path file, CompressionAlgorithm algorithm) throws IOException {
        try (TarArchiveSourceSpec spec = new TarArchiveSourceSpec(algorithm, file, ImmutableList.of(SHORT_TEXT_FILE, "unknownFile"))) {
            assertIsShortText(spec.getNextInternalSpec(), true, false, false);
            assertThrows(ArchiveEntryNotFoundException.class, spec::getNextInternalSpec);
        }
    }

}