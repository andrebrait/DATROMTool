package io.github.datromtool.io.copy.archive.impl;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.io.ArchiveContentsDependantTest;
import io.github.datromtool.io.compression.CompressionAlgorithm;
import io.github.datromtool.io.copy.archive.exceptions.ArchiveEntryNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
class TarArchiveSourceSpecTest extends ArchiveContentsDependantTest {

    static Stream<Arguments> tarArchives() {
        return Stream.concat(
                Stream.of(Arguments.of(archiveTestDataSource.resolve("archives").resolve("files.tar"), null)),
                Arrays.stream(CompressionAlgorithm.values())
                        .filter(a -> {
                            if (a.isEnabled()) {
                                return true;
                            } else {
                                log.warn("{} is disabled. Will not run tests for {}-compressed TAR archives.", a, a);
                                return false;
                            }
                        }).map(a -> Arguments.of(archiveTestDataSource.resolve("archives").resolve("files.tar." + a.getExtension()), a)));
    }

    /*
    Only testing mtime and ctime because BSD tar cannot preserve atime and GNU tar cannot preserve ctime
     */

    @ParameterizedTest
    @MethodSource("tarArchives")
    void testReadContents(Path file, CompressionAlgorithm algorithm) throws IOException {
        try (TarArchiveSourceSpec spec = new TarArchiveSourceSpec(algorithm, file)) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), false, false, DateField.MTIME);
            assertIsShortText(spec.getNextInternalSpec(), false, false, DateField.MTIME);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @ParameterizedTest
    @MethodSource("tarArchives")
    void testAllContentsInOrder(Path file, CompressionAlgorithm algorithm) throws IOException {
        try (TarArchiveSourceSpec spec = new TarArchiveSourceSpec(algorithm, file, ImmutableList.of(LOREM_IPSUM_FILE, SHORT_TEXT_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), false, false, DateField.MTIME);
            assertIsShortText(spec.getNextInternalSpec(), false, false, DateField.MTIME);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @ParameterizedTest
    @MethodSource("tarArchives")
    void testAllContentsInReverse_shouldStillBePhysicalOrder(Path file, CompressionAlgorithm algorithm) throws IOException {
        try (TarArchiveSourceSpec spec = new TarArchiveSourceSpec(algorithm, file, ImmutableList.of(SHORT_TEXT_FILE, LOREM_IPSUM_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), false, false, DateField.MTIME);
            assertIsShortText(spec.getNextInternalSpec(), false, false, DateField.MTIME);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @ParameterizedTest
    @MethodSource("tarArchives")
    void testReadOnlyLoremIpsum(Path file, CompressionAlgorithm algorithm) throws IOException {
        try (TarArchiveSourceSpec spec = new TarArchiveSourceSpec(algorithm, file, ImmutableList.of(LOREM_IPSUM_FILE))) {
            assertIsLoremIpsum(spec.getNextInternalSpec(), false, false, DateField.MTIME);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @ParameterizedTest
    @MethodSource("tarArchives")
    void testReadOnlyShortText(Path file, CompressionAlgorithm algorithm) throws IOException {
        try (TarArchiveSourceSpec spec = new TarArchiveSourceSpec(algorithm, file, ImmutableList.of(SHORT_TEXT_FILE))) {
            assertIsShortText(spec.getNextInternalSpec(), false, false, DateField.MTIME);
            assertNull(spec.getNextInternalSpec());
        }
    }

    @ParameterizedTest
    @MethodSource("tarArchives")
    void testReadShortTextAndThenUnknown(Path file, CompressionAlgorithm algorithm) throws IOException {
        try (TarArchiveSourceSpec spec = new TarArchiveSourceSpec(algorithm, file, ImmutableList.of(SHORT_TEXT_FILE, "unknownFile"))) {
            assertIsShortText(spec.getNextInternalSpec(), false, false, DateField.MTIME);
            assertThrows(ArchiveEntryNotFoundException.class, spec::getNextInternalSpec);
        }
    }

}