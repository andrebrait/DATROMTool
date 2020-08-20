package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.SortingPreference;

final class LanguageSubComparator extends SubComparator {

    private final ImmutableSet<String> languages;

    public LanguageSubComparator(SortingPreference sortingPreference) {
        super("Language selection");
        this.languages = sortingPreference.getLanguages();
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return compareIndices(o1, o2, ParsedGame::getLanguagesStream, languages);
    }
}
