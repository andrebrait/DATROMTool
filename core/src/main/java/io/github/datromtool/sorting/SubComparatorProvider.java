package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.SortingPreference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SubComparatorProvider {

    final static SubComparatorProvider INSTANCE = new SubComparatorProvider();

    public ImmutableList<SubComparator> toList(@Nonnull SortingPreference sortingPreference) {
        ImmutableList.Builder<SubComparator> subComparatorsBuilder = ImmutableList.builder();
        subComparatorsBuilder.add(new BadDumpSubComparator());
        if (sortingPreference.isPreferPrereleases()) {
            subComparatorsBuilder.add(new PreferReleasesSubComparator().reversed());
        }
        subComparatorsBuilder.add(new AvoidsListSubComparator(sortingPreference));
        if (sortingPreference.isPrioritizeLanguages()) {
            subComparatorsBuilder.add(new LanguageSubComparator(sortingPreference));
            subComparatorsBuilder.add(new RegionSubComparator(sortingPreference));
        } else {
            subComparatorsBuilder.add(new RegionSubComparator(sortingPreference));
            subComparatorsBuilder.add(new LanguageSubComparator(sortingPreference));
        }
        if (sortingPreference.isPreferParents()) {
            subComparatorsBuilder.add(new PreferParentsSubComparator());
        }
        subComparatorsBuilder.add(new PrefersListSubComparator(sortingPreference));
        subComparatorsBuilder.add(sortingPreference.isEarlyRevisions()
                ? new RevisionSubComparator()
                : new RevisionSubComparator().reversed());
        subComparatorsBuilder.add(sortingPreference.isEarlyVersions()
                ? new VersionSubComparator()
                : new VersionSubComparator().reversed());
        subComparatorsBuilder.add(new PreferReleasesSubComparator());
        subComparatorsBuilder.add(sortingPreference.isEarlyPrereleases()
                ? new SampleSubComparator()
                : new SampleSubComparator().reversed());
        subComparatorsBuilder.add(sortingPreference.isEarlyPrereleases()
                ? new DemoSubComparator()
                : new DemoSubComparator().reversed());
        subComparatorsBuilder.add(sortingPreference.isEarlyPrereleases()
                ? new BetaSubComparator()
                : new BetaSubComparator().reversed());
        subComparatorsBuilder.add(sortingPreference.isEarlyPrereleases()
                ? new ProtoSubComparator()
                : new ProtoSubComparator().reversed());
        subComparatorsBuilder.add(
                new SelectedLanguagesCountSubComparator(sortingPreference).reversed());
        subComparatorsBuilder.add(new LanguagesCountSubComparator().reversed());
        subComparatorsBuilder.add(new PreferParentsSubComparator());
        return subComparatorsBuilder.build();
    }

}
