package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableList;
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
        assertEquals(subComparators.size(), 15);
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
        assertEquals(subComparators.size(), 16);
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
        assertEquals(subComparators.size(), 15);
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
        assertEquals(subComparators.size(), 16);
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
    void testToList_earlyRevisions() {
        ImmutableList<SubComparator> subComparators =
                SubComparatorProvider.INSTANCE.toList(SortingPreference.builder()
                        .earlyRevisions(true)
                        .build());
        assertNotNull(subComparators);
        assertEquals(subComparators.size(), 15);
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
    void testToList_earlyVersions() {
        ImmutableList<SubComparator> subComparators =
                SubComparatorProvider.INSTANCE.toList(SortingPreference.builder()
                        .earlyVersions(true)
                        .build());
        assertNotNull(subComparators);
        assertEquals(subComparators.size(), 15);
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
    void testToList_earlyPrereleases() {
        ImmutableList<SubComparator> subComparators =
                SubComparatorProvider.INSTANCE.toList(SortingPreference.builder()
                        .earlyPrereleases(true)
                        .build());
        assertNotNull(subComparators);
        assertEquals(subComparators.size(), 15);
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
}