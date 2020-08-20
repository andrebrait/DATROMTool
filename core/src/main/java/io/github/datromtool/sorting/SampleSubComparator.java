package io.github.datromtool.sorting;

import io.github.datromtool.data.ParsedGame;

final class SampleSubComparator extends SubComparator {

    public SampleSubComparator() {
        super("Sample");
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return compareLists(o1, o2, ParsedGame::getSample);
    }
}
