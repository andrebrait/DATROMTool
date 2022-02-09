package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.ConfigDependantTest;
import io.github.datromtool.config.AppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileCopierTest extends ConfigDependantTest {

    private Path tempDir;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("datromtool_copy_test_");
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    @Test
    void testCopy() {
        FileScanner fs = new FileScanner(
                AppConfig.builder().build(),
                ImmutableList.of(),
                ImmutableList.of(),
                null);
        ImmutableList<FileScanner.Result> results = fs.scan(ImmutableList.of(testDataSource));
        Map<Path, List<FileScanner.Result>> resultsForArchive =
                results.stream().collect(Collectors.groupingBy(FileScanner.Result::getPath));
        ImmutableSet<FileCopier.Spec> specs = resultsForArchive.entrySet()
                .stream()
                .map(e -> {
                    ArchiveType archiveType = e.getValue()
                            .stream()
                            .map(FileScanner.Result::getArchiveType)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);
                    if (archiveType == null) {
                        return FileCopier.CopySpec.builder()
                                .from(e.getKey())
                                .to(tempDir.resolve(e.getKey().getFileName()))
                                .build();
                    }
                    if (archiveType == ArchiveType.RAR) {
                        return FileCopier.ArchiveCopySpec.builder()
                                .from(e.getKey())
                                .fromType(archiveType)
                                .to(tempDir.resolve(e.getKey()
                                        .getFileName()
                                        .toString()
                                        .replaceFirst("(?i)\\.rar$", ".rar.zip")))
                                .toType(ArchiveType.ZIP)
                                .internalSpecs(e.getValue().stream()
                                        .map(FileScanner.Result::getArchivePath)
                                        .map(p -> FileCopier.ArchiveCopySpec.InternalSpec.builder()
                                                .from(p)
                                                .to(p)
                                                .build())
                                        .collect(ImmutableSet.toImmutableSet()))
                                .build();
                    }
                    return FileCopier.ArchiveCopySpec.builder()
                            .from(e.getKey())
                            .fromType(archiveType)
                            .to(tempDir.resolve(e.getKey().getFileName()))
                            .toType(archiveType)
                            .internalSpecs(e.getValue().stream()
                                    .map(FileScanner.Result::getArchivePath)
                                    .map(p -> FileCopier.ArchiveCopySpec.InternalSpec.builder()
                                            .from(p)
                                            .to(p)
                                            .build())
                                    .collect(ImmutableSet.toImmutableSet()))
                            .build();
                }).collect(ImmutableSet.toImmutableSet());
        FileCopier fc = new FileCopier(AppConfig.builder().build(), false, null);
        fc.copy(specs);
        ImmutableList<FileScanner.Result> afterCopy = fs.scan(ImmutableList.of(tempDir));
        assertEquals(results.size(), afterCopy.size());
        assertAllResultsAreEqual(results, afterCopy);
    }

    private void assertAllResultsAreEqual(
            Collection<FileScanner.Result> results,
            Collection<FileScanner.Result> afterCopy) {
        for (FileScanner.Result r : results) {
            String filename = r.getPath().getFileName().toString();
            if (r.getArchiveType() == ArchiveType.RAR) {
                filename = filename.replaceFirst("(?i)\\.rar", ".zip");
            }
            for (FileScanner.Result r2 : afterCopy) {
                if (r2.getPath().endsWith(filename)) {
                    FileScanner.Result pathlessResult = r.withPath(Paths.get(""));
                    FileScanner.Result pathlessResult2 = r2.withPath(Paths.get(""));
                    if (r.getArchiveType() == ArchiveType.RAR) {
                        assertEquals(
                                pathlessResult.withArchiveType(ArchiveType.ZIP),
                                pathlessResult2);
                    } else {
                        assertEquals(pathlessResult, pathlessResult2);
                    }
                }
            }
        }
    }
}