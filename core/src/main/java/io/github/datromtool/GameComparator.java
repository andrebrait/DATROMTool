package io.github.datromtool;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
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
    
    private static int countMatches(String string, List<Pattern> patterns) {
        int matches = 0;
        for (Pattern p : patterns) {
            Matcher m1 = p.matcher(string);
            if (m1.find()) {
                matches += 1;
            }
        }
        return matches;
    }

    // TODO: store the result somewhere and retrieve it later
    private static int comparePatterns(ParsedGame o1, ParsedGame o2, List<Pattern> patterns) {
        int matches1 = countMatches(o1.getGame().getName(), patterns);
        int matches2 = countMatches(o2.getGame().getName(), patterns);;
        return Integer.compare(matches1, matches2);
    }

    private static int compareLists(List<Integer> l1, List<Integer> l2) {
        Iterator<Integer> i = l1.iterator();
        Iterator<Integer> j = l2.iterator();
        while (i.hasNext() || j.hasNext()) {
            int compareHasNext = Boolean.compare(i.hasNext(), j.hasNext());
            if (compareHasNext != 0) {
                return compareHasNext;
            }
            Integer itemA = i.next();
            Integer itemB = j.next();
            int compareInts = Integer.compare(itemA, itemB);
            if (compareInts != 0) {
                return compareInts;
            }
        }
        return 0;
    }

    private int compareParents(ParsedGame o1, ParsedGame o2) {
        if (preferParents) {
            return -Boolean.compare(o1.isParent(), o2.isParent());
        }
        return 0;
    }

    private int comparePrereleases(ParsedGame o1, ParsedGame o2) {
        if (preferPrereleases) {
            return -Boolean.compare(o1.isPrerelease(), o2.isPrerelease());
        }
        return 0;
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        int badCompare = Boolean.compare(o1.isBad(), o2.isBad());
        if (badCompare != 0) {
            return badCompare;
        }
        int prereleaseCompare = comparePrereleases(o1, o2);
        if (prereleaseCompare != 0) {
            return prereleaseCompare;
        }
        // Check avoid
        // Check languages if prefer language, region otherwise
        // Check region if prefer language, languages otherwise
        int parentCompare = compareParents(o1, o2);
        if (parentCompare != 0) {
            return parentCompare;
        }
        // Check prefer list
        // Check revision
        // Check version
        // Check sample
        // Check demo
        // Check beta
        // Check proto
        // Check how many languages it has (the more the better)
        return -Boolean.compare(o1.isParent(), o2.isParent());
    }
}
