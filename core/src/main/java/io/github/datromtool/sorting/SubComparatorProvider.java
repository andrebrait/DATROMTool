package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.OrderPreference;
import io.github.datromtool.data.SortingPreference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SubComparatorProvider {

    final static SubComparatorProvider INSTANCE = new SubComparatorProvider();

    public ImmutableList<SubComparator> toList(@Nonnull SortingPreference sortingPreference) {
        return toList(sortingPreference, false);
    }

    /**
     * Issue #19 step 2 overload: {@code clonelistPrioritiesPresent} registers
     * {@link PrioritySubComparator} right after the region comparator, so Retool clone-list-
     * assigned priorities ("1 is highest priority") are honored only when a clone list actually
     * supplied any for the current run. A run without clone list data calls
     * {@link #toList(SortingPreference)} above, which delegates here with {@code false} and stays
     * byte-identical to the pre-issue-#19 chain every other test in this suite pins.
     */
    public ImmutableList<SubComparator> toList(
            @Nonnull SortingPreference sortingPreference,
            boolean clonelistPrioritiesPresent) {
        ImmutableList.Builder<SubComparator> subComparatorsBuilder = ImmutableList.builder();
        subComparatorsBuilder.add(new BadDumpSubComparator());
        if (sortingPreference.isPreferPrereleases()) {
            subComparatorsBuilder.add(new PreferReleasesSubComparator().reversed());
        }
        subComparatorsBuilder.add(new AvoidsListSubComparator(sortingPreference));
        if (sortingPreference.isPrioritizeLanguages()) {
            subComparatorsBuilder.add(new LanguageSubComparator(sortingPreference));
            subComparatorsBuilder.add(new RegionSubComparator(sortingPreference));
            if (clonelistPrioritiesPresent) {
                subComparatorsBuilder.add(new PrioritySubComparator());
            }
        } else {
            subComparatorsBuilder.add(new RegionSubComparator(sortingPreference));
            if (clonelistPrioritiesPresent) {
                subComparatorsBuilder.add(new PrioritySubComparator());
            }
            subComparatorsBuilder.add(new LanguageSubComparator(sortingPreference));
        }
        if (sortingPreference.isPreferParents()) {
            subComparatorsBuilder.add(new PreferParentsSubComparator());
        }
        subComparatorsBuilder.add(new PrefersListSubComparator(sortingPreference));
        subComparatorsBuilder.add(sortingPreference.getRevisions() == OrderPreference.EARLIEST
                ? new RevisionSubComparator()
                : new RevisionSubComparator().reversed());
        subComparatorsBuilder.add(sortingPreference.getVersions() == OrderPreference.EARLIEST
                ? new VersionSubComparator()
                : new VersionSubComparator().reversed());
        subComparatorsBuilder.add(new PreferReleasesSubComparator());
        subComparatorsBuilder.add(sortingPreference.getPrereleases() == OrderPreference.EARLIEST
                ? new SampleSubComparator()
                : new SampleSubComparator().reversed());
        subComparatorsBuilder.add(sortingPreference.getPrereleases() == OrderPreference.EARLIEST
                ? new DemoSubComparator()
                : new DemoSubComparator().reversed());
        subComparatorsBuilder.add(sortingPreference.getPrereleases() == OrderPreference.EARLIEST
                ? new BetaSubComparator()
                : new BetaSubComparator().reversed());
        subComparatorsBuilder.add(sortingPreference.getPrereleases() == OrderPreference.EARLIEST
                ? new ProtoSubComparator()
                : new ProtoSubComparator().reversed());
        subComparatorsBuilder.add(
                new SelectedLanguagesCountSubComparator(sortingPreference).reversed());
        subComparatorsBuilder.add(new LanguagesCountSubComparator().reversed());
        subComparatorsBuilder.add(new PreferParentsSubComparator());
        return subComparatorsBuilder.build();
    }

}
