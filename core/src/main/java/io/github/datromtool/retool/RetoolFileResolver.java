package io.github.datromtool.retool;

import io.github.datromtool.SerializationHelper;
import io.github.datromtool.domain.retool.CloneList;
import io.github.datromtool.domain.retool.RetoolMetadata;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

/**
 * Resolves and loads a Retool clone list / metadata input source (issue #19 step 3), given
 * either an explicit file path or a directory to auto-match within by DAT header name - per the
 * issue's design ("auto-match data file to DAT by header name, like Retool does") and upstream's
 * own convention of naming a system's file {@code <DAT header name>.json} (e.g.
 * {@code "Atari - Atari 2600 (No-Intro).json"}, matching {@code Header#getName()}).
 *
 * <p>Deliberately stays in {@code core} (not {@code cli}): it needs no picocli type, only
 * {@link java.io.IOException}, which {@code cli}'s {@code OneGameOneRomCommand} wraps into a
 * {@link picocli.CommandLine.ParameterException} at the call site, exactly like every other
 * profile/DAT loading failure there.
 */
@NoArgsConstructor(access = PRIVATE)
public final class RetoolFileResolver {

    /**
     * The Retool clone list spec version this codebase's models ({@code domain.retool}) and
     * matching engine ({@link CloneListMatcher}) were implemented and verified against (issue
     * #19 steps 1-2) - pinned to the version this repo's own real, upstream-pinned fixtures
     * declare (core's {@code atari-2600-no-intro.json} and {@code nintendo-dsi-no-intro.json},
     * both {@code minimumVersion: "2.4.8"}). Per the issue's own contract-tracking requirement
     * ("our parsing contract must follow Retool's own published spec for these files... pin the
     * spec version we implement, keep fixtures of real upstream files"), this constant IS that
     * pin: it is DATROMTool's own declaration of which upstream clone list spec revision its
     * implementation understands, deliberately independent of DATROMTool's own release version
     * (see {@link #loadCloneList}'s Javadoc for why comparing against DATROMTool's version would
     * be meaningless). Bump it only after verifying this implementation against a newer upstream
     * fixture/spec page.
     */
    @Nonnull
    public static final String SUPPORTED_CLONELIST_SPEC_VERSION = "2.4.8";

    /**
     * Resolves {@code input} to an actual file: passed through unchanged if it already names a
     * regular file (explicit-file case), or auto-matched within it by {@code <headerName>.json}
     * if it names a directory. No other outcome is possible since callers only ever reach this
     * method with a path that {@code cli}'s {@code ExistingPathConverter} already confirmed is
     * an existing file or directory - so ambiguity (multiple candidate files) cannot arise: the
     * match is a single, deterministic exact-name lookup, not a search.
     *
     * <p><b>Security (review round):</b> {@code headerName} comes from the DAT's own
     * {@code <header><name>} - content a hostile DAT author fully controls, not a trusted local
     * value. Treated here as a bare file name only: rejected outright (before any filesystem
     * access) if it is an absolute path, contains a path separator ({@code /} or {@code \}), or
     * is exactly {@code ".."} or {@code "."} - closing both the relative traversal case (a
     * header of {@code "../evil"} escaping {@code input} one directory up) and the absolute-path
     * case ({@link Path#resolve(String)} discards {@code input} entirely when its argument is
     * itself absolute - documented behavior, not a bug in {@code resolve}). The normalized
     * candidate is then asserted to still be contained in {@code input} as a second, independent
     * check - defense in depth against any traversal shape the name-shape check above did not
     * anticipate.
     *
     * @throws IOException naming the offending {@code headerName} if it is not a valid bare file
     *                      name, or its resolved candidate would fall outside {@code input}; or
     *                      naming both {@code input} and {@code headerName} if {@code input} is a
     *                      directory with no matching file inside
     */
    @Nonnull
    public static Path resolveFile(@Nonnull Path input, @Nonnull String headerName) throws IOException {
        if (!Files.isDirectory(input)) {
            return input;
        }
        rejectUnsafeHeaderName(headerName);
        Path normalizedInput = input.normalize();
        Path candidate = normalizedInput.resolve(headerName + ".json").normalize();
        if (!candidate.startsWith(normalizedInput)) {
            throw new IOException(format(
                    "DAT header name '%s' resolves outside directory '%s' - refusing to use it",
                    headerName,
                    input));
        }
        if (!Files.isRegularFile(candidate)) {
            throw new IOException(format(
                    "No file matching DAT header name '%s' found in directory '%s' (expected '%s')",
                    headerName,
                    input,
                    candidate.getFileName()));
        }
        // startsWith above is lexical: a name inside the directory can still be a symlink out of
        // it, so containment has to hold once links are resolved too.
        if (!candidate.toRealPath().startsWith(normalizedInput.toRealPath())) {
            throw new IOException(format(
                    "DAT header name '%s' resolves outside directory '%s' through a link - refusing to use it",
                    headerName,
                    input));
        }
        return candidate;
    }

    // A header name the platform cannot even express as a path (NUL bytes, for instance) is
    // unsafe by definition; Paths.get would throw an unchecked InvalidPathException past our
    // IOException contract.
    private static boolean isAbsolutePath(String headerName) {
        try {
            return Paths.get(headerName).isAbsolute();
        } catch (InvalidPathException e) {
            return true;
        }
    }

    private static void rejectUnsafeHeaderName(String headerName) throws IOException {
        boolean unsafe = headerName.isEmpty()
                || headerName.contains("/")
                || headerName.contains("\\")
                || headerName.equals("..")
                || headerName.equals(".")
                || isAbsolutePath(headerName);
        if (unsafe) {
            throw new IOException(format(
                    "DAT header name '%s' is not a valid bare file name (must not be absolute, "
                            + "empty, or contain a path separator or '..'/'.')",
                    headerName));
        }
    }

    /**
     * Loads a clone list (auto-matching {@code input} first if it is a directory - see
     * {@link #resolveFile}), then enforces the {@code minimumVersion} compatibility gate (issue
     * #19 step 3): a clone list declaring a {@code minimumVersion} newer than {@link
     * #SUPPORTED_CLONELIST_SPEC_VERSION} is a hard failure, not a warning - a correctness tool
     * must not silently misapply a clone list authored against spec features it does not
     * implement.
     *
     * <p><b>Design rationale (corrected from this step's first pass):</b> the first pass compared
     * a clone list's {@code minimumVersion} against DATROMTool's own running application version
     * (from {@link SerializationHelper#getVersionString()}). That is meaningless: upstream's
     * {@code minimumVersion} names the minimum version of <em>Retool itself</em> required to
     * understand a clone list's features - a different project, versioned completely
     * independently of DATROMTool (whose own version was, at the time, {@code "1.0.0-RC3-
     * SNAPSHOT"} - lower than any real upstream clone list's {@code minimumVersion}, which would
     * have made every real clone list permanently rejected regardless of whether this
     * implementation actually supports its features). Per the issue's own contract-tracking
     * requirement ("pin the spec version we implement, keep fixtures of real upstream files"),
     * the gate instead compares against {@link #SUPPORTED_CLONELIST_SPEC_VERSION}: a constant
     * this implementation declares and controls, independent of DATROMTool's own version number
     * or build metadata - so no pre-release-suffix stripping is needed here either, unlike the
     * first pass.
     *
     * @throws IOException naming the file, its {@code minimumVersion}, and {@link
     *                      #SUPPORTED_CLONELIST_SPEC_VERSION} if incompatible - meaning the file
     *                      requires newer Retool clone list spec features than this DATROMTool
     *                      build implements - or wrapping/propagating a resolution or parse
     *                      failure
     */
    @Nonnull
    public static CloneList loadCloneList(
            @Nonnull Path input,
            @Nonnull String headerName) throws IOException {
        Path file = resolveFile(input, headerName);
        CloneList cloneList = SerializationHelper.getInstance().loadCloneList(file);
        String minimumVersion = cloneList.description().minimumVersion();
        if (!cloneList.isCompatibleWith(SUPPORTED_CLONELIST_SPEC_VERSION)) {
            throw new IOException(format(
                    "Clone list '%s' requires clone list spec version >= %s, but this DATROMTool "
                            + "build only implements up to spec version %s (the file requires "
                            + "newer Retool clone list features than this build supports)",
                    file,
                    minimumVersion,
                    SUPPORTED_CLONELIST_SPEC_VERSION));
        }
        return cloneList;
    }

    /**
     * Loads a Retool metadata file (auto-matching {@code input} first if it is a directory - see
     * {@link #resolveFile}). Metadata files carry no {@code minimumVersion} (see {@link
     * io.github.datromtool.domain.retool.RetoolMetadata}'s Javadoc), so there is no compatibility
     * gate here, unlike {@link #loadCloneList}.
     */
    @Nonnull
    public static RetoolMetadata loadRetoolMetadata(
            @Nonnull Path input,
            @Nonnull String headerName) throws IOException {
        Path file = resolveFile(input, headerName);
        return SerializationHelper.getInstance().loadRetoolMetadata(file);
    }
}
