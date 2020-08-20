package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.SortingPreference;

final class RegionSubComparator extends SubComparator {

    private final ImmutableSet<String> regions;

    public RegionSubComparator(SortingPreference sortingPreference) {
        super("Region selection");
        this.regions = sortingPreference.getRegions();
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        return compareIndices(o1, o2, ParsedGame::getRegionsStream, regions);
    }
}
