package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.SortingPreference;

import java.util.regex.Pattern;

final class AvoidsListSubComparator extends SubComparator {

    private final ImmutableSet<Pattern> avoids;

    public AvoidsListSubComparator(SortingPreference sortingPreference) {
        super("Avoids list");
        this.avoids = sortingPreference.getAvoids();
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return comparePatterns(o1, o2, avoids);
    }
}
