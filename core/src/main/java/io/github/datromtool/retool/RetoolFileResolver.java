package io.github.datromtool.retool;

import io.github.datromtool.SerializationHelper;
import io.github.datromtool.domain.retool.CloneList;
import io.github.datromtool.domain.retool.RetoolMetadata;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
     * Resolves {@code input} to an actual file: passed through unchanged if it already names a
     * regular file (explicit-file case), or auto-matched within it by {@code <headerName>.json}
     * if it names a directory. No other outcome is possible since callers only ever reach this
     * method with a path that {@code cli}'s {@code ExistingPathConverter} already confirmed is
     * an existing file or directory - so ambiguity (multiple candidate files) cannot arise: the
     * match is a single, deterministic exact-name lookup, not a search.
     *
     * @throws IOException naming both {@code input} and {@code headerName} if {@code input} is a
     *                      directory with no matching file inside
     */
    @Nonnull
    public static Path resolveFile(@Nonnull Path input, @Nonnull String headerName) throws IOException {
        if (!Files.isDirectory(input)) {
            return input;
        }
        Path candidate = input.resolve(headerName + ".json");
        if (!Files.isRegularFile(candidate)) {
            throw new IOException(format(
                    "No file matching DAT header name '%s' found in directory '%s' (expected '%s')",
                    headerName,
                    input,
                    candidate.getFileName()));
        }
        return candidate;
    }

    /**
     * Loads a clone list (auto-matching {@code input} first if it is a directory - see
     * {@link #resolveFile}), then enforces the {@code minimumVersion} compatibility gate (issue
     * #19 step 3): a clone list declaring a {@code minimumVersion} newer than {@code appVersion}
     * is a hard failure, not a warning - a correctness tool must not silently misapply a clone
     * list authored against features it does not implement.
     *
     * <p>{@code appVersion} is normalized with {@link #stripPrereleaseSuffix} before the compare,
     * since the real running value (from {@link SerializationHelper#getVersionString()}, e.g.
     * {@code "1.0.0-RC3-SNAPSHOT"}) carries a pre-release suffix that {@link
     * CloneList#isCompatibleWith}'s dot-segment comparison (issue #19 step 2, unchanged here)
     * does not parse as intended - splitting on {@code "."} leaves a trailing segment like
     * {@code "0-RC3-SNAPSHOT"}, which its non-numeric fallback compares lexicographically against
     * the clone list's plain-numeric segment, an accidental and unreliable result rather than a
     * considered one. Stripping once here, at the resolution boundary, keeps step 2's per-segment
     * fallback (which exists for a clone list's <em>own</em> {@code minimumVersion} value, a
     * separate concern) unchanged.
     *
     * <p><b>Known limitation, called out for the maintainer:</b> {@code minimumVersion} in the
     * upstream spec names the minimum version of <em>Retool itself</em>, not DATROMTool - the two
     * projects version independently. Real upstream clone lists (e.g. this repo's own pinned
     * fixture, {@code minimumVersion: "2.4.8"}) are therefore judged incompatible against
     * DATROMTool's actual pre-1.0 version until DATROMTool's own version number happens to reach
     * that number, which has no real bearing on whether this implementation understands the
     * file's features. This is implemented exactly as issue #19 step 3 specifies (compare against
     * the running DATROMTool version), since that is the documented, decided source - but it is
     * worth the maintainer revisiting whether the gate should instead compare against a dedicated,
     * independently-tracked "clone list spec version" this implementation claims to support.
     *
     * @throws IOException naming the file and both versions if incompatible, or wrapping/
     *                      propagating a resolution or parse failure
     */
    @Nonnull
    public static CloneList loadCloneList(
            @Nonnull Path input,
            @Nonnull String headerName,
            @Nonnull String appVersion) throws IOException {
        Path file = resolveFile(input, headerName);
        CloneList cloneList = SerializationHelper.getInstance().loadCloneList(file);
        String minimumVersion = cloneList.description().minimumVersion();
        if (!cloneList.isCompatibleWith(stripPrereleaseSuffix(appVersion))) {
            throw new IOException(format(
                    "Clone list '%s' requires DATROMTool >= %s, but this is DATROMTool %s",
                    file,
                    minimumVersion,
                    appVersion));
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

    /**
     * Strips a trailing pre-release suffix (e.g. {@code "-RC3-SNAPSHOT"}, {@code "-SNAPSHOT"},
     * {@code "-rc1"}) from a version string, for the numeric compare in {@link #loadCloneList}.
     * The rule is simply "everything before the first {@code '-'}": every pre-release marker this
     * project's own build produces (Maven {@code -SNAPSHOT}, release-candidate {@code -RCn}) is
     * hyphen-delimited, and a plain release version (e.g. {@code "2.5.0"}) has no hyphen at all,
     * so it passes through unchanged.
     */
    @Nonnull
    static String stripPrereleaseSuffix(@Nonnull String version) {
        int dash = version.indexOf('-');
        return dash < 0 ? version : version.substring(0, dash);
    }
}
