package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.ParsedGame;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Getter
public abstract class SubComparator implements Comparator<ParsedGame> {

    final static class ReversedSubComparator extends SubComparator {

        private final SubComparator delegate;

        private ReversedSubComparator(SubComparator delegate) {
            super(delegate.criteria + " (reversed)");
            this.delegate = delegate;
        }

        @Override
        public int compare(ParsedGame o1, ParsedGame o2) {
            return -delegate.compare(o1, o2);
        }
    }

    private final String criteria;

    protected SubComparator(String criteria) {
        this.criteria = criteria;
    }

    @Override
    public final SubComparator reversed() {
        return new ReversedSubComparator(this);
    }

    public final boolean isReverseOf(Class<? extends SubComparator> tClass) {
        return this instanceof ReversedSubComparator
                && tClass.isInstance(((ReversedSubComparator) this).delegate);
    }

    protected int comparePatterns(
            ParsedGame o1,
            ParsedGame o2,
            ImmutableCollection<Pattern> patterns) {
        int matches1 = computeMatches(o1, patterns);
        int matches2 = computeMatches(o2, patterns);
        return Integer.compare(matches1, matches2);
    }

    private int computeMatches(ParsedGame parsedGame, ImmutableCollection<Pattern> patterns) {
        int matches = countMatches(parsedGame.getGame().getName(), patterns);
        log.trace(
                "Obtained {} matches in patterns list {} for '{}'",
                matches,
                patterns,
                parsedGame.getGame().getName());
        return matches;
    }

    private int countMatches(String string, ImmutableCollection<Pattern> patterns) {
        return Math.toIntExact(patterns.stream()
                .map(p -> p.matcher(string))
                .filter(Matcher::find)
                .count());
    }

    protected <T> int compareIndices(
            ParsedGame o1,
            ParsedGame o2,
            Function<ParsedGame, Stream<T>> streamFunction,
            ImmutableCollection<T> collection) {
        int index1 = computeSmallestIndex(o1, streamFunction, collection);
        int index2 = computeSmallestIndex(o2, streamFunction, collection);
        return Integer.compare(index1, index2);
    }

    private <T> int computeSmallestIndex(
            ParsedGame parsedGame,
            Function<ParsedGame, Stream<T>> streamFunction,
            ImmutableCollection<T> collection) {
        int smallestIndex = smallestIndex(parsedGame, streamFunction, collection.asList());
        log.trace(
                "Smallest index {} found in list {} for '{}'",
                smallestIndex,
                collection,
                parsedGame);
        return smallestIndex;
    }

    private <K, T> int smallestIndex(
            K obj,
            Function<K, Stream<T>> streamFunction,
            ImmutableList<T> list) {
        return smallestIndex(streamFunction.apply(obj), list);
    }

    private <T> int smallestIndex(Stream<T> stream, ImmutableList<T> list) {
        return stream.mapToInt(list::indexOf)
                .filter(i -> i >= 0)
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    protected <T> int countUniqueMatches(
            ParsedGame parsedGame,
            Function<ParsedGame, Stream<T>> streamFunction,
            Predicate<T> predicate) {
        return streamFunction.apply(parsedGame)
                .filter(predicate)
                .collect(Collectors.toSet())
                .size();
    }

    protected <K, T extends Comparable<T>> int compareLists(
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
            int compareItems = itemA.compareTo(itemB);
            if (compareItems != 0) {
                return compareItems;
            }
        }
        return 0;
    }

}
