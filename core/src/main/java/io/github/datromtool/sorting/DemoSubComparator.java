package io.github.datromtool.sorting;

import io.github.datromtool.data.ParsedGame;

final class DemoSubComparator extends SubComparator {

    public DemoSubComparator() {
        super("Demo");
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return compareLists(o1, o2, ParsedGame::getDemo);
    }
}
