package io.github.datromtool.sorting;

import io.github.datromtool.data.ParsedGame;

final class LanguagesCountSubComparator extends SubComparator {

    public LanguagesCountSubComparator() {
        super("Total languages count");
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return Integer.compare(
                countUniqueMatches(
                        o1,
                        ParsedGame::getLanguagesStream,
                        s -> true),
                countUniqueMatches(
                        o2,
                        ParsedGame::getLanguagesStream,
                        s -> true));
    }
}
