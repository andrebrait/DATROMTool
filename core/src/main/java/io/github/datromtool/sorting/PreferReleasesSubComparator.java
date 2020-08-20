package io.github.datromtool.sorting;

import io.github.datromtool.data.ParsedGame;

final class PreferReleasesSubComparator extends SubComparator {

    public PreferReleasesSubComparator() {
        super("Prefer releases");
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return Boolean.compare(o1.isPrerelease(), o2.isPrerelease());
    }
}
