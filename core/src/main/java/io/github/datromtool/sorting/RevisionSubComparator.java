package io.github.datromtool.sorting;

import io.github.datromtool.data.ParsedGame;

final class RevisionSubComparator extends SubComparator {

    public RevisionSubComparator() {
        super("Revision");
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return compareLists(o1, o2, ParsedGame::getRevision);
    }
}
