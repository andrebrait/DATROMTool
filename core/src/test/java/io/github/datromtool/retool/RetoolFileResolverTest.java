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
 *
 * <p><b>Correction round:</b> the {@code minimumVersion} gate compares against {@link
 * RetoolFileResolver#SUPPORTED_CLONELIST_SPEC_VERSION} (a constant this codebase declares and
 * controls), not DATROMTool's own running version - see that constant's Javadoc for why. This
 * repo's real upstream fixture ({@code atari-2600-no-intro.json}, {@code minimumVersion:
 * "2.4.8"}) is therefore compatible (it is, in fact, the fixture the constant is pinned from);
 * the incompatible-rejection test instead uses a small synthetic fixture demanding a
 * deliberately unreachable {@code minimumVersion} ({@code "99.0.0"}).
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

    // --- resolveFile: path traversal (SECURITY, review round) - headerName comes from the
    // DAT's <header><name>, external content a hostile DAT author fully controls. Executed
    // before this fix: a header of "../evil" resolved a file one directory above the intended
    // clone list/metadata directory; an absolute-path header discarded the directory entirely
    // (java.nio.file.Path#resolve's documented behavior for an absolute argument); a
    // separator-containing header could reach a subdirectory the directory auto-match contract
    // never intended to search. All three must now be rejected with a message naming the
    // offending header, before any filesystem access against the resolved candidate.

    @Test
    void resolveFileRejectsParentTraversalHeaderName(@TempDir Path root) throws IOException {
        Path input = Files.createDirectory(root.resolve("input"));
        // The traversal target: a file one directory above `input`, reachable via "../evil".
        Files.writeString(root.resolve("evil.json"), "{}");

        IOException ex = assertThrows(
                IOException.class,
                () -> RetoolFileResolver.resolveFile(input, "../evil"));
        assertTrue(
                ex.getMessage().contains("../evil"),
                "error must name the offending header, got: " + ex.getMessage());
    }

    @Test
    void resolveFileRejectsAbsolutePathHeaderName(@TempDir Path root) throws IOException {
        Path input = Files.createDirectory(root.resolve("input"));
        Path outside = Files.createDirectory(root.resolve("outside"));
        Files.writeString(outside.resolve("secret.json"), "{}");
        String absoluteHeaderName = outside.resolve("secret").toString();

        IOException ex = assertThrows(
                IOException.class,
                () -> RetoolFileResolver.resolveFile(input, absoluteHeaderName));
        assertTrue(
                ex.getMessage().contains(absoluteHeaderName),
                "error must name the offending header, got: " + ex.getMessage());
    }

    @Test
    void resolveFileRejectsSeparatorContainingHeaderName(@TempDir Path dir) throws IOException {
        Path sub = Files.createDirectory(dir.resolve("sub"));
        Files.writeString(sub.resolve("evil.json"), "{}");

        IOException ex = assertThrows(
                IOException.class,
                () -> RetoolFileResolver.resolveFile(dir, "sub/evil"));
        assertTrue(
                ex.getMessage().contains("sub/evil"),
                "error must name the offending header, got: " + ex.getMessage());
    }

    // Discriminating control: a header name with no separators/traversal still resolves
    // normally - proves the rejection above is about the offending shape, not a blanket
    // regression on directory auto-match.
    @Test
    void resolveFileStillAutoMatchesOrdinaryHeaderNameAfterTraversalFix(@TempDir Path dir) throws IOException {
        Path expected = dir.resolve("Atari - Atari 2600 (No-Intro).json");
        Files.writeString(expected, "{}");

        Path resolved = RetoolFileResolver.resolveFile(dir, "Atari - Atari 2600 (No-Intro)");
        assertEquals(expected, resolved);
    }

    // --- loadCloneList: minimumVersion compatibility gate, against SUPPORTED_CLONELIST_SPEC_VERSION ---

    // Frozen red-first proof (correction round): the real upstream fixture (minimumVersion
    // "2.4.8") must load successfully. RED on the interim/pre-correction code (which compared
    // against DATROMTool's own running app version, ~"1.0.0" - lower than "2.4.8", so this threw);
    // GREEN once the gate compares against SUPPORTED_CLONELIST_SPEC_VERSION ("2.4.8") instead.
    @Test
    void realUpstreamFixtureLoadsSuccessfully() throws Exception {
        Path file = resource("retool/clonelists/atari-2600-no-intro.json");
        CloneList cloneList = RetoolFileResolver.loadCloneList(file, "Atari - Atari 2600 (No-Intro)");
        assertEquals("Atari - Atari 2600 (No-Intro)", cloneList.description().name());
    }

    @Test
    void loadCloneListRejectsIncompatibleMinimumVersion() throws Exception {
        Path file = resource("retool/clonelists/huge-minimum-version.json");
        IOException ex = assertThrows(
                IOException.class,
                () -> RetoolFileResolver.loadCloneList(file, "Huge Minimum Version Test"));
        assertTrue(ex.getMessage().contains(file.toString()), "error must name the file, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("99.0.0"), "error must name the clone list's minimumVersion, got: " + ex.getMessage());
        assertTrue(
                ex.getMessage().contains(RetoolFileResolver.SUPPORTED_CLONELIST_SPEC_VERSION),
                "error must name the supported spec version, got: " + ex.getMessage());
    }

    @Test
    void loadCloneListAcceptsCompatibleMinimumVersion() throws Exception {
        Path file = resource("retool/clonelists/atari-2600-no-intro.json");
        CloneList cloneList = RetoolFileResolver.loadCloneList(file, "Atari - Atari 2600 (No-Intro)");
        assertEquals("Atari - Atari 2600 (No-Intro)", cloneList.description().name());
    }

    @Test
    void loadCloneListAutoMatchesThenChecksCompatibility(@TempDir Path dir) throws Exception {
        Path source = resource("retool/clonelists/atari-2600-no-intro.json");
        Path target = dir.resolve("Atari - Atari 2600 (No-Intro).json");
        Files.copy(source, target);

        CloneList cloneList = RetoolFileResolver.loadCloneList(dir, "Atari - Atari 2600 (No-Intro)");
        assertEquals("Atari - Atari 2600 (No-Intro)", cloneList.description().name());
    }

    @Test
    void loadCloneListAutoMatchesThenRejectsIncompatible(@TempDir Path dir) throws Exception {
        Path source = resource("retool/clonelists/huge-minimum-version.json");
        Path target = dir.resolve("Huge Minimum Version Test.json");
        Files.copy(source, target);

        IOException ex = assertThrows(
                IOException.class,
                () -> RetoolFileResolver.loadCloneList(dir, "Huge Minimum Version Test"));
        assertTrue(ex.getMessage().contains("99.0.0"));
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
        CloneList viaResolver = RetoolFileResolver.loadCloneList(file, "Atari - Atari 2600 (No-Intro)");
        assertEquals(direct, viaResolver);
    }
    @Test
    void resolveFileRejectsHeaderNameThePlatformCannotExpressAsAPath(@TempDir Path dir) {
        // A NUL byte cannot appear in a real Logiqx header, but the guard must still answer with
        // its documented IOException rather than an unchecked InvalidPathException.
        IOException thrown = assertThrows(
                IOException.class,
                () -> RetoolFileResolver.resolveFile(dir, "\u0000evil"),
                "a header name that is not expressible as a path must be rejected as unsafe");
        assertTrue(
                thrown.getMessage().contains("not a valid bare file name"),
                "rejection must use the unsafe-header-name message, got: " + thrown.getMessage());
    }
}
