package io.github.datromtool;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.SortingPreference;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GameComparator implements Comparator<ParsedGame> {

    private final static Logger logger = LoggerFactory.getLogger(GameComparator.class);

    private final ImmutableList<Comparation> comparations;

    public GameComparator(@Nonnull SortingPreference sortingPreference) {
        Objects.requireNonNull(sortingPreference);
        this.comparations = ImmutableList.of(
                new Comparation(
                        "Bad dump",
                        (o1, o2) -> Boolean.compare(o1.isBad(), o2.isBad())),
                new Comparation(
                        "Prefer prereleases",
                        sortingPreference.isPreferPrereleases()
                                ? (o1, o2) -> -Boolean.compare(o1.isPrerelease(), o2.isPrerelease())
                                : (o1, o2) -> 0),
                new Comparation(
                        "Avoids list",
                        (o1, o2) -> comparePatterns(o1, o2, sortingPreference.getAvoids())),
                sortingPreference.isPrioritizeLanguages()
                        ? languageSelectionComparation(sortingPreference)
                        : regionSelectionComparation(sortingPreference),
                sortingPreference.isPrioritizeLanguages()
                        ? regionSelectionComparation(sortingPreference)
                        : languageSelectionComparation(sortingPreference),
                new Comparation(
                        "Prefer parents",
                        sortingPreference.isPreferParents()
                                ? (o1, o2) -> -Boolean.compare(o1.isParent(), o2.isParent())
                                : (o1, o2) -> 0),
                new Comparation(
                        "Prefers list",
                        (o1, o2) -> -comparePatterns(o1, o2, sortingPreference.getPrefers())),
                ascendingIfTrue(
                        sortingPreference.isEarlyRevisions(),
                        new Comparation(
                                "Revision",
                                (o1, o2) -> compareLists(o1, o2, ParsedGame::getRevision))),
                ascendingIfTrue(
                        sortingPreference.isEarlyVersions(),
                        new Comparation(
                                "Version",
                                (o1, o2) -> compareLists(o1, o2, ParsedGame::getVersion))),
                new Comparation(
                        "Prefer releases",
                        (o1, o2) -> Boolean.compare(o1.isPrerelease(), o2.isPrerelease())),
                ascendingIfTrue(
                        sortingPreference.isEarlyPrereleases(),
                        new Comparation(
                                "Sample",
                                (o1, o2) -> compareLists(o1, o2, ParsedGame::getSample))),
                ascendingIfTrue(
                        sortingPreference.isEarlyPrereleases(),
                        new Comparation(
                                "Demo",
                                (o1, o2) -> compareLists(o1, o2, ParsedGame::getDemo))),
                ascendingIfTrue(
                        sortingPreference.isEarlyPrereleases(),
                        new Comparation(
                                "Beta",
                                (o1, o2) -> compareLists(o1, o2, ParsedGame::getBeta))),
                ascendingIfTrue(
                        sortingPreference.isEarlyPrereleases(),
                        new Comparation(
                                "Proto",
                                (o1, o2) -> compareLists(o1, o2, ParsedGame::getProto))),
                new Comparation(
                        "Selected languages count (Descending)",
                        (o1, o2) -> -Long.compare(
                                countLanguageMatches(sortingPreference, o1),
                                countLanguageMatches(sortingPreference, o2))),
                new Comparation(
                        "Number of languages (Descending)",
                        (o1, o2) -> -Integer.compare(
                                uniqueLanguagesCount(o1),
                                uniqueLanguagesCount(o2))),
                new Comparation(
                        "Parent",
                        (o1, o2) -> -Boolean.compare(o1.isParent(), o2.isParent())));
    }

    private Comparation regionSelectionComparation(SortingPreference sortingPreference) {
        return new Comparation(
                "Region selection",
                (o1, o2) -> compareRegions(o1, o2, sortingPreference));
    }

    private Comparation languageSelectionComparation(SortingPreference sortingPreference) {
        return new Comparation(
                "Language selection",
                (o1, o2) -> compareLanguage(o1, o2, sortingPreference));
    }

    private long countLanguageMatches(SortingPreference sortingPreference, ParsedGame g) {
        return g.getLanguagesStream()
                .filter(sortingPreference.getLanguages()::contains)
                .count();
    }

    private int uniqueLanguagesCount(ParsedGame o1) {
        return o1.getLanguagesStream().collect(Collectors.toSet()).size();
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        for (Comparation comparation : comparations) {
            int result = comparation.getFunction().apply(o1, o2);
            if (result != 0) {
                logger.debug(
                        "Under criteria '{}', '{}' is preferred over '{}'",
                        comparation.getCriteria(),
                        result < 0 ? o1.getGame().getName() : o2.getGame().getName(),
                        result < 0 ? o2.getGame().getName() : o1.getGame().getName());
                return result;
            }
        }
        logger.debug(
                "'{}' and '{}' are equal under every criteria",
                o1.getGame().getName(),
                o2.getGame().getName());
        return 0;
    }

    @Value
    private static class Comparation {

        String criteria;
        BiFunction<ParsedGame, ParsedGame, Integer> function;
    }

    private int comparePatterns(
            ParsedGame o1,
            ParsedGame o2,
            ImmutableCollection<Pattern> patterns) {
        int matches1 = computeMatches(o1, patterns);
        int matches2 = computeMatches(o2, patterns);
        return Integer.compare(matches1, matches2);
    }

    private int computeMatches(ParsedGame parsedGame, ImmutableCollection<Pattern> patterns) {
        int matches = countMatches(parsedGame.getGame().getName(), patterns);
        logger.trace(
                "Obtained {} matches in patterns list {} for '{}'",
                matches,
                patterns,
                parsedGame.getGame().getName());
        return matches;
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
        int index1 = computeSmallestIndex(o1, streamFunction, collection);
        int index2 = computeSmallestIndex(o2, streamFunction, collection);
        return Integer.compare(index1, index2);
    }

    private int computeSmallestIndex(
            ParsedGame parsedGame,
            Function<ParsedGame, Stream<String>> streamFunction,
            ImmutableCollection<String> collection) {
        int smallestIndex = smallestIndex(parsedGame, streamFunction, collection.asList());
        logger.trace(
                "Smallest index {} found in list {} for '{}'",
                smallestIndex,
                collection,
                parsedGame);
        return smallestIndex;
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
        return ascending ?
                comparation :
                new Comparation(
                        comparation.getCriteria() + " (Descending)",
                        (o1, o2) -> -comparation.getFunction().apply(o1, o2));
    }
}
