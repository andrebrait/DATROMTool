package io.github.datromtool.retool;

import io.github.datromtool.SerializationHelper;
import io.github.datromtool.domain.retool.CloneList;
import io.github.datromtool.domain.retool.RetoolMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage matrix rows 3 (minimumVersion compatibility) and 4 (directory auto-match) for issue
 * #19 step 3's {@link RetoolFileResolver}.
 */
class RetoolFileResolverTest {

    private static Path resource(String name) throws URISyntaxException {
        return Paths.get(ClassLoader.getSystemResource(name).toURI());
    }

    // --- resolveFile: explicit file passes through unchanged, headerName is irrelevant ---

    @Test
    void resolveFileReturnsExplicitFileUnchanged() throws Exception {
        Path file = resource("retool/clonelists/atari-2600-no-intro.json");
        assertEquals(file, RetoolFileResolver.resolveFile(file, "Some Completely Different Name"));
    }

    // --- resolveFile: directory auto-match by DAT header name ---

    @Test
    void resolveFileAutoMatchesByHeaderNameInDirectory(@TempDir Path dir) throws IOException {
        Path expected = dir.resolve("Atari - Atari 2600 (No-Intro).json");
        Files.writeString(expected, "{}");
        Files.writeString(dir.resolve("Some Other System (No-Intro).json"), "{}");

        Path resolved = RetoolFileResolver.resolveFile(dir, "Atari - Atari 2600 (No-Intro)");
        assertEquals(expected, resolved);
    }

    @Test
    void resolveFileMissingMatchThrowsClearError(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("Some Other System (No-Intro).json"), "{}");

        IOException ex = assertThrows(
                IOException.class,
                () -> RetoolFileResolver.resolveFile(dir, "Atari - Atari 2600 (No-Intro)"));
        assertTrue(ex.getMessage().contains(dir.toString()), "error must name the directory searched, got: " + ex.getMessage());
        assertTrue(
                ex.getMessage().contains("Atari - Atari 2600 (No-Intro)"),
                "error must name the header name searched, got: " + ex.getMessage());
    }

    // --- stripPrereleaseSuffix ---

    @Test
    void stripPrereleaseSuffixStripsRcAndSnapshot() {
        assertEquals("1.0.0", RetoolFileResolver.stripPrereleaseSuffix("1.0.0-RC3-SNAPSHOT"));
        assertEquals("1.0.0", RetoolFileResolver.stripPrereleaseSuffix("1.0.0-SNAPSHOT"));
        assertEquals("1.0.0", RetoolFileResolver.stripPrereleaseSuffix("1.0.0-rc1"));
        assertEquals("2.5.0", RetoolFileResolver.stripPrereleaseSuffix("2.5.0"));
    }

    // --- loadCloneList: minimumVersion compatibility gate ---

    @Test
    void loadCloneListRejectsIncompatibleMinimumVersion() throws Exception {
        Path file = resource("retool/clonelists/atari-2600-no-intro.json");
        // The real upstream fixture declares minimumVersion 2.4.8; our synthetic "running app
        // version" here is deliberately far below it, mirroring this codebase's own real
        // pre-1.0 version (see RetoolFileResolver's Javadoc "known limitation" note for why
        // that real-world comparison is itself surprising).
        IOException ex = assertThrows(
                IOException.class,
                () -> RetoolFileResolver.loadCloneList(file, "Atari - Atari 2600 (No-Intro)", "1.0.0-RC3-SNAPSHOT"));
        assertTrue(ex.getMessage().contains(file.toString()), "error must name the file, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("2.4.8"), "error must name the clone list's minimumVersion, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("1.0.0-RC3-SNAPSHOT"), "error must name the running app version, got: " + ex.getMessage());
    }

    @Test
    void loadCloneListAcceptsCompatibleMinimumVersion() throws Exception {
        Path file = resource("retool/clonelists/atari-2600-no-intro.json");
        CloneList cloneList = RetoolFileResolver.loadCloneList(file, "Atari - Atari 2600 (No-Intro)", "999.0.0");
        assertEquals("Atari - Atari 2600 (No-Intro)", cloneList.description().name());
    }

    @Test
    void loadCloneListAutoMatchesThenChecksCompatibility(@TempDir Path dir) throws Exception {
        Path source = resource("retool/clonelists/atari-2600-no-intro.json");
        Path target = dir.resolve("Atari - Atari 2600 (No-Intro).json");
        Files.copy(source, target);

        IOException ex = assertThrows(
                IOException.class,
                () -> RetoolFileResolver.loadCloneList(dir, "Atari - Atari 2600 (No-Intro)", "1.0.0"));
        assertTrue(ex.getMessage().contains("2.4.8"));
    }

    // --- loadRetoolMetadata: no compatibility gate, same auto-match ---

    @Test
    void loadRetoolMetadataAutoMatches(@TempDir Path dir) throws Exception {
        Path source = resource("retool/metadata/atari-2600-no-intro-trimmed.json");
        Path target = dir.resolve("Atari - Atari 2600 (No-Intro).json");
        Files.copy(source, target);

        RetoolMetadata metadata = RetoolFileResolver.loadRetoolMetadata(dir, "Atari - Atari 2600 (No-Intro)");
        assertTrue(metadata.entries().containsKey("Air Raiders (USA)"));
    }

    @Test
    void loadRetoolMetadataExplicitFileBypassesMatching() throws Exception {
        Path file = resource("retool/metadata/atari-2600-no-intro-trimmed.json");
        RetoolMetadata metadata = RetoolFileResolver.loadRetoolMetadata(file, "Totally Unrelated Header Name");
        assertTrue(metadata.entries().containsKey("Air Raiders (USA)"));
    }

    // Sanity: SerializationHelper's own loader is what backs this, not reimplemented here.
    @Test
    void loadCloneListDelegatesToSerializationHelper() throws Exception {
        Path file = resource("retool/clonelists/atari-2600-no-intro.json");
        CloneList direct = SerializationHelper.getInstance().loadCloneList(file);
        CloneList viaResolver = RetoolFileResolver.loadCloneList(file, "Atari - Atari 2600 (No-Intro)", "999.0.0");
        assertEquals(direct, viaResolver);
    }
}
