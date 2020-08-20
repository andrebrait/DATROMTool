package io.github.datromtool.sorting;

import io.github.datromtool.data.ParsedGame;

final class BadDumpSubComparator extends SubComparator {

    public BadDumpSubComparator() {
        super("Bad dump");
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return Boolean.compare(o1.isBad(), o2.isBad());
    }
}
