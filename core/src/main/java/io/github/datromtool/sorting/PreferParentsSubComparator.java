package io.github.datromtool.sorting;

import io.github.datromtool.data.ParsedGame;

final class PreferParentsSubComparator extends SubComparator {

    public PreferParentsSubComparator() {
        super("Prefer parents");
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return -Boolean.compare(o1.isParent(), o2.isParent());
    }
}
