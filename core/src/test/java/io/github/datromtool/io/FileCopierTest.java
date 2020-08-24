package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.config.AppConfig;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileCopierTest {

    private static final String TEST_DATA_FOLDER = "./test-data";
    private Path testDataSource;
    private Path tempDir;

    @BeforeMethod
    public void setup() throws Exception {
        tempDir = Files.createTempDirectory("datromtool_copy_test");
        String testDir = System.getenv("DATROMTOOL_TEST_DIR");
        if (testDir == null) {
            if (Files.isDirectory(Paths.get(TEST_DATA_FOLDER))) {
                testDir = TEST_DATA_FOLDER;
            } else {
                testDir = System.getProperty("java.io.tmpdir") + "/datromtool_copy_test_data";
            }
        }
        testDataSource = Paths.get(testDir, "data");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    @Test
    public void testCopy() throws Exception {
        FileScanner fs = new FileScanner(AppConfig.builder().build(), null, null, null);
        ImmutableList<FileScanner.Result> results = fs.scan(testDataSource);
        Map<Path, List<FileScanner.Result>> resultsForArchive =
                results.stream().collect(Collectors.groupingBy(FileScanner.Result::getPath));
        ImmutableSet<FileCopier.CopyDefinition> copyDefinitions = resultsForArchive.entrySet()
                .stream()
                .map(e -> {
                    ArchiveType archiveType = e.getValue()
                            .stream()
                            .map(FileScanner.Result::getArchiveType)
                            .filter(at -> at != ArchiveType.NONE)
                            .findFirst()
                            .orElse(ArchiveType.NONE);
                    return FileCopier.CopyDefinition.builder()
                            .from(e.getKey())
                            .to(tempDir.resolve(e.getKey()
                                    .getFileName()
                                    .toString()
                                    .replaceFirst("(?i)\\.rar4\\.rar$", ".rar4.zip")))
                            .fromType(archiveType)
                            .archiveCopyDefinitions(
                                    archiveType == ArchiveType.NONE
                                            ? ImmutableSet.of()
                                            : e.getValue().stream()
                                                    .map(i -> FileCopier.ArchiveCopyDefinition.builder()
                                                            .source(i.getArchivePath())
                                                            .destination(Paths.get(i.getArchivePath())
                                                                    .getFileName()
                                                                    .toString())
                                                            .build()
                                                    ).collect(ImmutableSet.toImmutableSet()))
                            .build();
                }).collect(ImmutableSet.toImmutableSet());
        FileCopier fc = new FileCopier(AppConfig.builder().build(), false, null);
        fc.copy(copyDefinitions);
        ImmutableList<FileScanner.Result> afterCopy = fs.scan(tempDir);
    }
}