package io.github.datromtool;

import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

public abstract class TestDirDependantTest {

    protected static final String TEST_DATA_FOLDER = "../test-data";
    protected static Path testDir;
    protected static Path scanTestDataSource;
    protected static Path archiveTestDataSource;

    @BeforeAll
    static void setupTestDataSource() {
        String testDirStr = System.getenv("DATROMTOOL_TEST_DIR");
        if (testDirStr == null && Files.isDirectory(Paths.get(TEST_DATA_FOLDER))) {
            testDirStr = TEST_DATA_FOLDER;
        }
        testDir = Paths.get(requireNonNull(testDirStr));
        scanTestDataSource = testDir.resolve("data").resolve("scan-test-files");
        archiveTestDataSource = testDir.resolve("data").resolve("archive-test-files");
    }
}
