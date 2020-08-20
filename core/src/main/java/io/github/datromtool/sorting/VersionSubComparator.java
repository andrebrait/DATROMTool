package io.github.datromtool.sorting;

import io.github.datromtool.data.ParsedGame;

final class VersionSubComparator extends SubComparator {

    public VersionSubComparator() {
        super("Version");
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return compareLists(o1, o2, ParsedGame::getVersion);
    }
}
