package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.SortingPreference;

final class SelectedLanguagesCountSubComparator extends SubComparator {

    private final ImmutableSet<String> languages;

    public SelectedLanguagesCountSubComparator(SortingPreference sortingPreference) {
        super("Selected languages count");
        this.languages = sortingPreference.getLanguages();
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return Integer.compare(
                countUniqueMatches(
                        o1,
                        ParsedGame::getLanguagesStream,
                        languages::contains),
                countUniqueMatches(
                        o2,
                        ParsedGame::getLanguagesStream,
                        languages::contains));
    }
}
