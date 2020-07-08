package io.github.datromtool;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.SortingPreference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Value
@Builder(toBuilder = true)
public class GameComparator implements Comparator<ParsedGame> {

    ConcurrentMap<Pair<ParsedGame, ?>, Integer> indicesMap = new ConcurrentHashMap<>();

    @NonNull
    SortingPreference sortingPreference;

    @Value
    private static class Pair<K, T> {

        K left;
        ImmutableCollection<T> right;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pair)) {
                return false;
            }
            Pair<?, ?> pair = (Pair<?, ?>) o;
            // Identity comparison is all we need for this cache
            return left == pair.left
                    && right == pair.right;
        }

        @Override
        public int hashCode() {
            // Identity hashing is all we need for this cache
            return 31 * System.identityHashCode(left) + System.identityHashCode(right);
        }
    }

    private int comparePatterns(
            ParsedGame o1,
            ParsedGame o2,
            ImmutableCollection<Pattern> patterns) {
        int matches1 = indicesMap.computeIfAbsent(
                new Pair<>(o1, patterns),
                k -> countMatches(k.getLeft().getGame().getName(), patterns));
        int matches2 = indicesMap.computeIfAbsent(
                new Pair<>(o2, patterns),
                k -> countMatches(k.getLeft().getGame().getName(), patterns));
        return Integer.compare(matches1, matches2);
    }

    private static int countMatches(String string, ImmutableCollection<Pattern> patterns) {
        return (int) patterns.stream()
                .map(p -> p.matcher(string))
                .filter(Matcher::find)
                .count();
    }

    private int compareLanguage(ParsedGame o1, ParsedGame o2) {
        return compareIndices(
                o1,
                o2,
                ParsedGame::getLanguagesStream,
                sortingPreference.getLanguages());
    }

    private int compareRegions(ParsedGame o1, ParsedGame o2) {
        return compareIndices(
                o1,
                o2,
                ParsedGame::getRegionsStream,
                sortingPreference.getRegions());
    }

    private int compareIndices(
            ParsedGame o1,
            ParsedGame o2,
            Function<ParsedGame, Stream<String>> streamFunction,
            ImmutableCollection<String> collection) {
        int index1 = indicesMap.computeIfAbsent(
                new Pair<>(o1, collection),
                k -> smallestIndex(o1, streamFunction, collection.asList()));
        int index2 = indicesMap.computeIfAbsent(
                new Pair<>(o2, collection),
                k -> smallestIndex(o2, streamFunction, collection.asList()));
        return Integer.compare(index1, index2);
    }

    private static <K, T> int smallestIndex(
            K obj,
            Function<K, Stream<T>> streamFunction,
            ImmutableList<T> list) {
        return smallestIndex(streamFunction.apply(obj), list);
    }

    private static <T> int smallestIndex(Stream<T> stream, ImmutableList<T> list) {
        return stream.mapToInt(list::indexOf)
                .filter(i -> i >= 0)
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    private static <K, T extends Comparable<T>> int compareLists(
            K obj1,
            K obj2,
            Function<K, ImmutableCollection<T>> collectionFunction) {
        Iterator<T> i = collectionFunction.apply(obj1).iterator();
        Iterator<T> j = collectionFunction.apply(obj2).iterator();
        while (i.hasNext() || j.hasNext()) {
            int compareHasNext = Boolean.compare(i.hasNext(), j.hasNext());
            if (compareHasNext != 0) {
                return compareHasNext;
            }
            T itemA = i.next();
            T itemB = j.next();
            int compareInts = itemA.compareTo(itemB);
            if (compareInts != 0) {
                return compareInts;
            }
        }
        return 0;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        int badCompare = Boolean.compare(o1.isBad(), o2.isBad());
        if (badCompare != 0) {
            return badCompare;
        }
        if (sortingPreference.isPreferPrereleases()) {
            int prereleaseCompare = Boolean.compare(o1.isPrerelease(), o2.isPrerelease());
            if (prereleaseCompare != 0) {
                return -prereleaseCompare;
            }
        }
        int hasAvoids = comparePatterns(o1, o2, sortingPreference.getAvoids());
        if (hasAvoids != 0) {
            return hasAvoids;
        }
        if (sortingPreference.isPrioritizeLanguages()) {
            int languageCompare = compareLanguage(o1, o2);
            if (languageCompare != 0) {
                return languageCompare;
            }
            int regionCompare = compareRegions(o1, o2);
            if (regionCompare == 0) {
                return regionCompare;
            }
        } else {
            int regionCompare = compareRegions(o1, o2);
            if (regionCompare == 0) {
                return regionCompare;
            }
            int languageCompare = compareLanguage(o1, o2);
            if (languageCompare != 0) {
                return languageCompare;
            }
        }
        if (sortingPreference.isPreferParents()) {
            int parentCompare = Boolean.compare(o1.isParent(), o2.isParent());
            if (parentCompare != 0) {
                return -parentCompare;
            }
        }
        int hasPrefers = comparePatterns(o1, o2, sortingPreference.getPrefers());
        if (hasPrefers != 0) {
            return -hasPrefers;
        }
        int compareRevision = compareLists(o1, o2, ParsedGame::getRevision);
        if (compareRevision != 0) {
            return sortingPreference.isEarlyRevisions() ? compareRevision : -compareRevision;
        }
        int compareVersion = compareLists(o1, o2, ParsedGame::getVersion);
        if (compareVersion != 0) {
            return sortingPreference.isEarlyVersions() ? compareVersion : -compareVersion;
        }
        int compareSample = compareLists(o1, o2, ParsedGame::getSample);
        if (compareSample != 0) {
            return sortingPreference.isEarlyPrereleases() ? compareSample : -compareSample;
        }
        int compareDemo = compareLists(o1, o2, ParsedGame::getDemo);
        if (compareDemo != 0) {
            return sortingPreference.isEarlyPrereleases() ? compareDemo : -compareDemo;
        }
        int compareBeta = compareLists(o1, o2, ParsedGame::getBeta);
        if (compareBeta != 0) {
            return sortingPreference.isEarlyPrereleases() ? compareBeta : -compareBeta;
        }
        int compareProto = compareLists(o1, o2, ParsedGame::getProto);
        if (compareProto != 0) {
            return sortingPreference.isEarlyPrereleases() ? compareProto : -compareProto;
        }
        int compareLanguagesSize =
                Integer.compare(o1.getLanguages().size(), o2.getLanguages().size());
        if (compareLanguagesSize != 0) {
            return -compareLanguagesSize;
        }
        return -Boolean.compare(o1.isParent(), o2.isParent());
    }
}
