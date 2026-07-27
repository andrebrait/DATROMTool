package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.OrderPreference;
import io.github.datromtool.data.SortingPreference;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubComparatorProviderTest {

    @Test
    void testToList_defaultOptions() {
        ImmutableList<SubComparator> subComparators =
                SubComparatorProvider.INSTANCE.toList(SortingPreference.builder().build());
        assertNotNull(subComparators);
        assertEquals(15, subComparators.size());
        Iterator<SubComparator> i = subComparators.iterator();
        assertTrue(i.next() instanceof BadDumpSubComparator);
        assertTrue(i.next() instanceof AvoidsListSubComparator);
        assertTrue(i.next() instanceof RegionSubComparator);
        assertTrue(i.next() instanceof LanguageSubComparator);
        assertTrue(i.next() instanceof PrefersListSubComparator);
        assertTrue(i.next().isReverseOf(RevisionSubComparator.class));
        assertTrue(i.next().isReverseOf(VersionSubComparator.class));
        assertTrue(i.next() instanceof PreferReleasesSubComparator);
        assertTrue(i.next().isReverseOf(SampleSubComparator.class));
        assertTrue(i.next().isReverseOf(DemoSubComparator.class));
        assertTrue(i.next().isReverseOf(BetaSubComparator.class));
        assertTrue(i.next().isReverseOf(ProtoSubComparator.class));
        assertTrue(i.next().isReverseOf(SelectedLanguagesCountSubComparator.class));
        assertTrue(i.next().isReverseOf(LanguagesCountSubComparator.class));
        assertTrue(i.next() instanceof PreferParentsSubComparator);
        assertFalse(i.hasNext());
    }

    @Test
    void testToList_preferPrereleases() {
        ImmutableList<SubComparator> subComparators =
                SubComparatorProvider.INSTANCE.toList(SortingPreference.builder()
                        .preferPrereleases(true)
                        .build());
        assertNotNull(subComparators);
        assertEquals(16, subComparators.size());
        Iterator<SubComparator> i = subComparators.iterator();
        assertTrue(i.next() instanceof BadDumpSubComparator);
        assertTrue(i.next().isReverseOf(PreferReleasesSubComparator.class));
        assertTrue(i.next() instanceof AvoidsListSubComparator);
        assertTrue(i.next() instanceof RegionSubComparator);
        assertTrue(i.next() instanceof LanguageSubComparator);
        assertTrue(i.next() instanceof PrefersListSubComparator);
        assertTrue(i.next().isReverseOf(RevisionSubComparator.class));
        assertTrue(i.next().isReverseOf(VersionSubComparator.class));
        assertTrue(i.next() instanceof PreferReleasesSubComparator);
        assertTrue(i.next().isReverseOf(SampleSubComparator.class));
        assertTrue(i.next().isReverseOf(DemoSubComparator.class));
        assertTrue(i.next().isReverseOf(BetaSubComparator.class));
        assertTrue(i.next().isReverseOf(ProtoSubComparator.class));
        assertTrue(i.next().isReverseOf(SelectedLanguagesCountSubComparator.class));
        assertTrue(i.next().isReverseOf(LanguagesCountSubComparator.class));
        assertTrue(i.next() instanceof PreferParentsSubComparator);
        assertFalse(i.hasNext());
    }

    @Test
    void testToList_prioritizeLanguages() {
        ImmutableList<SubComparator> subComparators =
                SubComparatorProvider.INSTANCE.toList(SortingPreference.builder()
                        .prioritizeLanguages(true)
                        .build());
        assertNotNull(subComparators);
        assertEquals(15, subComparators.size());
        Iterator<SubComparator> i = subComparators.iterator();
        assertTrue(i.next() instanceof BadDumpSubComparator);
        assertTrue(i.next() instanceof AvoidsListSubComparator);
        assertTrue(i.next() instanceof LanguageSubComparator);
        assertTrue(i.next() instanceof RegionSubComparator);
        assertTrue(i.next() instanceof PrefersListSubComparator);
        assertTrue(i.next().isReverseOf(RevisionSubComparator.class));
        assertTrue(i.next().isReverseOf(VersionSubComparator.class));
        assertTrue(i.next() instanceof PreferReleasesSubComparator);
        assertTrue(i.next().isReverseOf(SampleSubComparator.class));
        assertTrue(i.next().isReverseOf(DemoSubComparator.class));
        assertTrue(i.next().isReverseOf(BetaSubComparator.class));
        assertTrue(i.next().isReverseOf(ProtoSubComparator.class));
        assertTrue(i.next().isReverseOf(SelectedLanguagesCountSubComparator.class));
        assertTrue(i.next().isReverseOf(LanguagesCountSubComparator.class));
        assertTrue(i.next() instanceof PreferParentsSubComparator);
        assertFalse(i.hasNext());
    }

    @Test
    void testToList_preferParents() {
        ImmutableList<SubComparator> subComparators =
                SubComparatorProvider.INSTANCE.toList(SortingPreference.builder()
                        .preferParents(true)
                        .build());
        assertNotNull(subComparators);
        assertEquals(16, subComparators.size());
        Iterator<SubComparator> i = subComparators.iterator();
        assertTrue(i.next() instanceof BadDumpSubComparator);
        assertTrue(i.next() instanceof AvoidsListSubComparator);
        assertTrue(i.next() instanceof RegionSubComparator);
        assertTrue(i.next() instanceof LanguageSubComparator);
        assertTrue(i.next() instanceof PreferParentsSubComparator);
        assertTrue(i.next() instanceof PrefersListSubComparator);
        assertTrue(i.next().isReverseOf(RevisionSubComparator.class));
        assertTrue(i.next().isReverseOf(VersionSubComparator.class));
        assertTrue(i.next() instanceof PreferReleasesSubComparator);
        assertTrue(i.next().isReverseOf(SampleSubComparator.class));
        assertTrue(i.next().isReverseOf(DemoSubComparator.class));
        assertTrue(i.next().isReverseOf(BetaSubComparator.class));
        assertTrue(i.next().isReverseOf(ProtoSubComparator.class));
        assertTrue(i.next().isReverseOf(SelectedLanguagesCountSubComparator.class));
        assertTrue(i.next().isReverseOf(LanguagesCountSubComparator.class));
        assertTrue(i.next() instanceof PreferParentsSubComparator);
        assertFalse(i.hasNext());
    }

    @Test
    void testToList_revisionsEarliest() {
        ImmutableList<SubComparator> subComparators =
                SubComparatorProvider.INSTANCE.toList(SortingPreference.builder()
                        .revisions(OrderPreference.EARLIEST)
                        .build());
        assertNotNull(subComparators);
        assertEquals(15, subComparators.size());
        Iterator<SubComparator> i = subComparators.iterator();
        assertTrue(i.next() instanceof BadDumpSubComparator);
        assertTrue(i.next() instanceof AvoidsListSubComparator);
        assertTrue(i.next() instanceof RegionSubComparator);
        assertTrue(i.next() instanceof LanguageSubComparator);
        assertTrue(i.next() instanceof PrefersListSubComparator);
        assertTrue(i.next() instanceof RevisionSubComparator);
        assertTrue(i.next().isReverseOf(VersionSubComparator.class));
        assertTrue(i.next() instanceof PreferReleasesSubComparator);
        assertTrue(i.next().isReverseOf(SampleSubComparator.class));
        assertTrue(i.next().isReverseOf(DemoSubComparator.class));
        assertTrue(i.next().isReverseOf(BetaSubComparator.class));
        assertTrue(i.next().isReverseOf(ProtoSubComparator.class));
        assertTrue(i.next().isReverseOf(SelectedLanguagesCountSubComparator.class));
        assertTrue(i.next().isReverseOf(LanguagesCountSubComparator.class));
        assertTrue(i.next() instanceof PreferParentsSubComparator);
        assertFalse(i.hasNext());
    }

    @Test
    void testToList_versionsEarliest() {
        ImmutableList<SubComparator> subComparators =
                SubComparatorProvider.INSTANCE.toList(SortingPreference.builder()
                        .versions(OrderPreference.EARLIEST)
                        .build());
        assertNotNull(subComparators);
        assertEquals(15, subComparators.size());
        Iterator<SubComparator> i = subComparators.iterator();
        assertTrue(i.next() instanceof BadDumpSubComparator);
        assertTrue(i.next() instanceof AvoidsListSubComparator);
        assertTrue(i.next() instanceof RegionSubComparator);
        assertTrue(i.next() instanceof LanguageSubComparator);
        assertTrue(i.next() instanceof PrefersListSubComparator);
        assertTrue(i.next().isReverseOf(RevisionSubComparator.class));
        assertTrue(i.next() instanceof VersionSubComparator);
        assertTrue(i.next() instanceof PreferReleasesSubComparator);
        assertTrue(i.next().isReverseOf(SampleSubComparator.class));
        assertTrue(i.next().isReverseOf(DemoSubComparator.class));
        assertTrue(i.next().isReverseOf(BetaSubComparator.class));
        assertTrue(i.next().isReverseOf(ProtoSubComparator.class));
        assertTrue(i.next().isReverseOf(SelectedLanguagesCountSubComparator.class));
        assertTrue(i.next().isReverseOf(LanguagesCountSubComparator.class));
        assertTrue(i.next() instanceof PreferParentsSubComparator);
        assertFalse(i.hasNext());
    }

    @Test
    void testToList_prereleasesEarliest() {
        ImmutableList<SubComparator> subComparators =
                SubComparatorProvider.INSTANCE.toList(SortingPreference.builder()
                        .prereleases(OrderPreference.EARLIEST)
                        .build());
        assertNotNull(subComparators);
        assertEquals(15, subComparators.size());
        Iterator<SubComparator> i = subComparators.iterator();
        assertTrue(i.next() instanceof BadDumpSubComparator);
        assertTrue(i.next() instanceof AvoidsListSubComparator);
        assertTrue(i.next() instanceof RegionSubComparator);
        assertTrue(i.next() instanceof LanguageSubComparator);
        assertTrue(i.next() instanceof PrefersListSubComparator);
        assertTrue(i.next().isReverseOf(RevisionSubComparator.class));
        assertTrue(i.next().isReverseOf(VersionSubComparator.class));
        assertTrue(i.next() instanceof PreferReleasesSubComparator);
        assertTrue(i.next() instanceof SampleSubComparator);
        assertTrue(i.next() instanceof DemoSubComparator);
        assertTrue(i.next() instanceof BetaSubComparator);
        assertTrue(i.next() instanceof ProtoSubComparator);
        assertTrue(i.next().isReverseOf(SelectedLanguagesCountSubComparator.class));
        assertTrue(i.next().isReverseOf(LanguagesCountSubComparator.class));
        assertTrue(i.next() instanceof PreferParentsSubComparator);
        assertFalse(i.hasNext());
    }

    // Issue #19 step 2: the 2-arg overload with clonelistPrioritiesPresent=false must delegate to
    // exactly the same chain as the 1-arg overload above (byte-identical, same 15-comparator
    // list).
    //
    // Review round (test honesty): the previous version of this test only compared list sizes,
    // which passes even if the two overloads produced the same 15 comparators in a different
    // order (e.g. a future edit that makes the 1-arg overload stop delegating to the 2-arg one
    // and instead duplicate its logic with a transposed pair) - a real divergence between the
    // two overloads that a size-only check cannot catch. Strengthened to compare the full
    // ordered comparator sequence, via each comparator's own `criteria` description (which
    // already encodes reversed-ness too - see SubComparator.ReversedSubComparator), not just
    // `getClass()` (which would not distinguish *what* a ReversedSubComparator wraps).
    @Test
    void testToList_clonelistPrioritiesAbsent_matchesSingleArgOverload() {
        ImmutableList<SubComparator> withFlag =
                SubComparatorProvider.INSTANCE.toList(SortingPreference.builder().build(), false);
        ImmutableList<SubComparator> withoutFlag =
                SubComparatorProvider.INSTANCE.toList(SortingPreference.builder().build());
        assertEquals(15, withFlag.size());
        assertEquals(
                withoutFlag.stream().map(SubComparator::getCriteria).toList(),
                withFlag.stream().map(SubComparator::getCriteria).toList(),
                "the 2-arg overload with clonelistPrioritiesPresent=false must produce the "
                        + "identical *ordered* comparator chain as the 1-arg overload, not just "
                        + "the same length");
    }

    @Test
    void testToList_clonelistPrioritiesPresent_insertsPriorityRightAfterRegion() {
        ImmutableList<SubComparator> subComparators = SubComparatorProvider.INSTANCE.toList(
                SortingPreference.builder().build(),
                true);
        assertEquals(16, subComparators.size());
        Iterator<SubComparator> i = subComparators.iterator();
        assertTrue(i.next() instanceof BadDumpSubComparator);
        assertTrue(i.next() instanceof AvoidsListSubComparator);
        assertTrue(i.next() instanceof RegionSubComparator);
        assertTrue(i.next() instanceof PrioritySubComparator);
        assertTrue(i.next() instanceof LanguageSubComparator);
        assertTrue(i.next() instanceof PrefersListSubComparator);
    }

    @Test
    void testToList_clonelistPrioritiesPresent_insertsPriorityRightAfterRegion_prioritizeLanguages() {
        ImmutableList<SubComparator> subComparators = SubComparatorProvider.INSTANCE.toList(
                SortingPreference.builder().prioritizeLanguages(true).build(),
                true);
        assertEquals(16, subComparators.size());
        Iterator<SubComparator> i = subComparators.iterator();
        assertTrue(i.next() instanceof BadDumpSubComparator);
        assertTrue(i.next() instanceof AvoidsListSubComparator);
        assertTrue(i.next() instanceof LanguageSubComparator);
        assertTrue(i.next() instanceof RegionSubComparator);
        assertTrue(i.next() instanceof PrioritySubComparator);
        assertTrue(i.next() instanceof PrefersListSubComparator);
    }
}