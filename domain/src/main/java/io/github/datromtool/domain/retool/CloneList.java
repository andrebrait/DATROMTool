package io.github.datromtool.domain.retool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

/**
 * Root type of a Retool clone list JSON file, per
 * <a href="https://unexpectedpanda.github.io/retool/contribute-clone-lists-top-level-keys/">
 * contribute-clone-lists-top-level-keys</a>: "{@code description (obj[str, str])} - Required"
 * and "{@code variants (array[obj])} - Optional. All keys are optional, except for
 * {@code description}."
 *
 * <p>Unknown JSON fields anywhere under this type are tolerated, not rejected - the shared
 * {@code jsonMapper} in {@code SerializationHelper} has {@code FAIL_ON_UNKNOWN_PROPERTIES}
 * disabled - since upstream clone list files carry fields (and whole sub-shapes, e.g. future
 * spec additions) this step does not yet model.
 */
@JsonInclude(NON_DEFAULT)
public record CloneList(
        @Nonnull
        @JsonProperty(required = true)
        CloneListDescription description,

        @Nonnull
        ImmutableList<VariantGroup> variants) {

    public CloneList {
        if (variants == null) variants = ImmutableList.of();
    }

    /**
     * Compatibility check for {@link CloneListDescription#minimumVersion()} (issue #19 step 2):
     * compares this clone list's declared {@code minimumVersion} against a running DATROMTool
     * version. This is a pure check - no enforcement (rejecting or warning on an incompatible
     * clone list at load time) is wired up yet; that is issue #19 step 3's job, which owns
     * threading clone lists into the CLI/profile flow in the first place.
     *
     * <p>Versions are compared component-wise as dot-separated segments (e.g. {@code "2.4.8"}):
     * each segment is parsed as a non-negative integer when possible, and compared numerically;
     * a non-numeric segment (e.g. a {@code -rc1} suffix) falls back to a lexicographic compare of
     * just that segment. A version with fewer segments than the other is padded with {@code "0"}
     * segments (so {@code "2.4"} is treated as equal to {@code "2.4.0"}).
     *
     * @param appVersion the running DATROMTool version to check, e.g. {@code "2.5.0"}
     * @return {@code true} if {@code appVersion} is greater than or equal to
     *         {@link CloneListDescription#minimumVersion()}, i.e. this clone list is safe to use
     */
    public boolean isCompatibleWith(@Nonnull String appVersion) {
        return compareVersions(appVersion, description.minimumVersion()) >= 0;
    }

    private static int compareVersions(String a, String b) {
        String[] segmentsA = a.split("\\.");
        String[] segmentsB = b.split("\\.");
        int length = Math.max(segmentsA.length, segmentsB.length);
        for (int i = 0; i < length; i++) {
            String segmentA = i < segmentsA.length ? segmentsA[i] : "0";
            String segmentB = i < segmentsB.length ? segmentsB[i] : "0";
            int comparison = compareSegments(segmentA, segmentB);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static int compareSegments(String a, String b) {
        try {
            return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }
}
