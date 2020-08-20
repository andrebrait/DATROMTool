package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.SortingPreference;

import java.util.regex.Pattern;

final class PrefersListSubComparator extends SubComparator {

    private final ImmutableSet<Pattern> prefers;

    public PrefersListSubComparator(SortingPreference sortingPreference) {
        super("Prefers list");
        this.prefers = sortingPreference.getPrefers();
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return -comparePatterns(o1, o2, prefers);
    }
}
