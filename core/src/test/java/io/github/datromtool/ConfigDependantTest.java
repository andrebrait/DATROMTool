package io.github.datromtool;

import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

public abstract class ConfigDependantTest {

    static protected final String TEST_DATA_FOLDER = "../test-data";
    static protected Path testDir;
    static protected Path testDataSource;

    @BeforeAll
    static void setupTestDataSource() {
        String testDirStr = System.getenv("DATROMTOOL_TEST_DIR");
        if (testDirStr == null && Files.isDirectory(Paths.get(TEST_DATA_FOLDER))) {
            testDirStr = TEST_DATA_FOLDER;
        }
        testDir = Paths.get(requireNonNull(testDirStr));
        testDataSource = testDir.resolve("data").resolve("files");
    }
}
