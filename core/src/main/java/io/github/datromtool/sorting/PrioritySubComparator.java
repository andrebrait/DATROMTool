package io.github.datromtool.sorting;

import io.github.datromtool.data.ParsedGame;

/**
 * Compares by Retool clone list priority (issue #19 step 2): upstream's {@code VariantTitle}/
 * {@code VariantFilter.Results} spec states "priority (int, default 1)" with 1 as the highest
 * priority, so lower numbers sort first here. Upstream's full scoping statement (verbatim):
 * "Priorities for titles are only taken into account for titles in the same region, with same
 * group and short name."
 *
 * <p>This implementation honors two of those three scopes, not all three:
 * <ul>
 *   <li><b>group</b> is enforced structurally, by pipeline construction, not by this comparator:
 *       {@link io.github.datromtool.GameSorter} groups games by {@link
 *       ParsedGame#getClonelistGroup()} (falling back to DAT parent/clone) before this comparator
 *       ever runs, so it only ever compares games already known to share a group.</li>
 *   <li><b>region</b> is enforced by this comparator's <em>position</em> in the chain, right
 *       after {@link RegionSubComparator} in {@link
 *       SubComparatorProvider#toList(io.github.datromtool.data.SortingPreference, boolean)}:
 *       region has already been resolved by the time priority is consulted, in both branches of
 *       that chain (regardless of whether language selection is prioritized ahead of or behind
 *       region), since the spec only speaks to priority being compared within the same region,
 *       not relative to language.</li>
 *   <li><b>short name</b> equality is <em>not</em> separately enforced anywhere in this chain - a
 *       deliberate simplification, not a spec-faithful port: two titles matched into the same
 *       clone list group can have different short names under this implementation (see {@link
 *       io.github.datromtool.retool.CloneListMatcher}'s {@code NameType#SHORT} Javadoc for how
 *       loosely "short name" is approximated here) and still have their priorities compared
 *       against each other. Revisiting this gate is out of scope for issue #19 - it matters most
 *       once {@code supersets} (deferred alongside this) are acted on, since upstream's
 *       short-name scoping exists precisely to keep a variant's priority from leaking across
 *       titles that only coincidentally share a group.</li>
 * </ul>
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
