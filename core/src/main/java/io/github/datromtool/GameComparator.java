package io.github.datromtool;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.SortingPreference;
import lombok.Value;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class GameComparator implements Comparator<ParsedGame> {

    private final ConcurrentMap<Pair<ParsedGame, ?>, Integer> operationsCache =
            new ConcurrentHashMap<>();

    private final ImmutableList<Comparation> comparations;

    public GameComparator(@Nonnull SortingPreference sortingPreference) {
        Objects.requireNonNull(sortingPreference);
        this.comparations = ImmutableList.of(
                (o1, o2) -> Boolean.compare(o1.isBad(), o2.isBad()),
                sortingPreference.isPreferPrereleases()
                        ? (o1, o2) -> -Boolean.compare(o1.isPrerelease(), o2.isPrerelease())
                        : (o1, o2) -> 0,
                (o1, o2) -> comparePatterns(o1, o2, sortingPreference.getAvoids()),
                sortingPreference.isPrioritizeLanguages()
                        ? (o1, o2) -> compareLanguage(o1, o2, sortingPreference)
                        : (o1, o2) -> compareRegions(o1, o2, sortingPreference),
                sortingPreference.isPrioritizeLanguages()
                        ? (o1, o2) -> compareRegions(o1, o2, sortingPreference)
                        : (o1, o2) -> compareLanguage(o1, o2, sortingPreference),
                sortingPreference.isPreferParents()
                        ? (o1, o2) -> -Boolean.compare(o1.isParent(), o2.isParent())
                        : (o1, o2) -> 0,
                (o1, o2) -> -comparePatterns(o1, o2, sortingPreference.getPrefers()),
                ascendingIfTrue(
                        sortingPreference.isEarlyVersions(),
                        (o1, o2) -> compareLists(o1, o2, ParsedGame::getVersion)),
                ascendingIfTrue(
                        sortingPreference.isEarlyVersions(),
                        (o1, o2) -> compareLists(o1, o2, ParsedGame::getVersion)),
                ascendingIfTrue(
                        sortingPreference.isEarlyPrereleases(),
                        (o1, o2) -> compareLists(o1, o2, ParsedGame::getSample)),
                ascendingIfTrue(
                        sortingPreference.isEarlyPrereleases(),
                        (o1, o2) -> compareLists(o1, o2, ParsedGame::getDemo)),
                ascendingIfTrue(
                        sortingPreference.isEarlyPrereleases(),
                        (o1, o2) -> compareLists(o1, o2, ParsedGame::getBeta)),
                ascendingIfTrue(
                        sortingPreference.isEarlyPrereleases(),
                        (o1, o2) -> compareLists(o1, o2, ParsedGame::getProto)),
                (o1, o2) -> -Integer.compare(o1.getLanguages().size(), o2.getLanguages().size()),
                (o1, o2) -> -Boolean.compare(o1.isParent(), o2.isParent()));
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        for (Comparation comparation : comparations) {
            int result = comparation.compare(o1, o2);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    private interface Comparation {

        int compare(ParsedGame o1, ParsedGame o2);
    }

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
        int matches1 = operationsCache.computeIfAbsent(
                new Pair<>(o1, patterns),
                k -> countMatches(k.getLeft().getGame().getName(), patterns));
        int matches2 = operationsCache.computeIfAbsent(
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

    private int compareLanguage(ParsedGame o1, ParsedGame o2, SortingPreference sortingPreference) {
        return compareIndices(
                o1,
                o2,
                ParsedGame::getLanguagesStream,
                sortingPreference.getLanguages());
    }

    private int compareRegions(ParsedGame o1, ParsedGame o2, SortingPreference sortingPreference) {
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
        int index1 = operationsCache.computeIfAbsent(
                new Pair<>(o1, collection),
                k -> smallestIndex(o1, streamFunction, collection.asList()));
        int index2 = operationsCache.computeIfAbsent(
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

    private static Comparation ascendingIfTrue(boolean ascending, Comparation comparation) {
        return ascending ? comparation : (o1, o2) -> -comparation.compare(o1, o2);
    }
}
