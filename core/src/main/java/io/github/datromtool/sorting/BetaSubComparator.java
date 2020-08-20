package io.github.datromtool.sorting;

import io.github.datromtool.data.ParsedGame;

final class BetaSubComparator extends SubComparator {

    public BetaSubComparator() {
        super("Beta");
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return compareLists(o1, o2, ParsedGame::getBeta);
    }
}
