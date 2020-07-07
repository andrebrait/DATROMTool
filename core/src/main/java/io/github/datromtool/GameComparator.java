package io.github.datromtool;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Comparator;
import java.util.regex.Pattern;

@Value
@Builder(toBuilder = true)
public class GameComparator implements Comparator<ParsedGame> {

    @NonNull
    @Builder.Default
    ImmutableSet<Pattern> prefers = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableSet<Pattern> avoids = ImmutableSet.of();

    @Builder.Default
    boolean prioritizeLanguages = false;

    @Builder.Default
    boolean earlyVersions = false;

    @Builder.Default
    boolean earlyRevisions = false;

    @Builder.Default
    boolean earlyPrereleases = false;

    @Builder.Default
    boolean preferParents = false;

    @Builder.Default
    boolean preferPrereleases = false;

    private static int compareParents(ParsedGame o1, ParsedGame o2, boolean preferParents) {
        if (preferParents && o1.isParent() != o2.isParent()) {
            if (o1.isParent()) {
                return -1;
            }
            return 1;
        }
        return 0;
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        // TODO sort by other things first
        int parentCompare = compareParents(o1, o2, preferParents);
        if (parentCompare != 0) {
            return parentCompare;
        }
        // TODO sort by other things between these two
        int secondParentCompare = compareParents(o1, o2, true);
        if (secondParentCompare != 0) {
            return secondParentCompare;
        }
        return 0;
    }
}
