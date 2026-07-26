package io.github.datromtool.sorting;

import io.github.datromtool.data.ParsedGame;

/**
 * Compares by Retool clone list priority (issue #19 step 2): upstream's {@code VariantTitle}/
 * {@code VariantFilter.Results} spec states "priority (int, default 1)" with 1 as the highest
 * priority, so lower numbers sort first here. Positioned right after {@link RegionSubComparator}
 * in {@link SubComparatorProvider#toList(io.github.datromtool.data.SortingPreference, boolean)}:
 * Retool compares a variant's priority within the already-selected region, not before it. Kept
 * immediately adjacent to region selection in both branches of that chain (regardless of whether
 * language selection is prioritized ahead of or behind region), since the spec only speaks to
 * priority being compared "within the same region", not relative to language.
 *
 * <p>Neutral (returns {@code 0}) whenever either compared game lacks a clone-list-assigned
 * priority ({@link ParsedGame#getClonelistPriority()} is {@code null}). In practice this
 * comparator is only registered in the sub-comparator chain when a clone list actually supplied
 * data for the current run (see {@link SubComparatorProvider}), but it stays neutral for
 * unmatched games even then.
 */
final class PrioritySubComparator extends SubComparator {

    PrioritySubComparator() {
        super("Clone list priority");
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        Integer p1 = o1.getClonelistPriority();
        Integer p2 = o2.getClonelistPriority();
        if (p1 == null || p2 == null) {
            return 0;
        }
        return Integer.compare(p1, p2);
    }
}
