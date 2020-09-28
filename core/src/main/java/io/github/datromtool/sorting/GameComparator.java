package io.github.datromtool.sorting;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.SortingPreference;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Comparator;

@Slf4j
public final class GameComparator implements Comparator<ParsedGame> {

    private final ImmutableList<SubComparator> subComparators;

    public GameComparator(@Nonnull SortingPreference sortingPreference) {
        this.subComparators = SubComparatorProvider.INSTANCE.toList(sortingPreference);
    }

    @VisibleForTesting
    GameComparator(
            @Nonnull SubComparatorProvider provider,
            @Nonnull SortingPreference sortingPreference) {
        this.subComparators = provider.toList(sortingPreference);
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        for (SubComparator subComparator : subComparators) {
            int result = subComparator.compare(o1, o2);
            if (result != 0) {
                log.debug(
                        "Under criteria '{}', '{}' is preferred over '{}'",
                        subComparator.getCriteria(),
                        result < 0 ? o1.getGame().getName() : o2.getGame().getName(),
                        result < 0 ? o2.getGame().getName() : o1.getGame().getName());
                return result;
            }
        }
        log.debug(
                "'{}' and '{}' are equal under every criteria",
                o1.getGame().getName(),
                o2.getGame().getName());
        return 0;
    }
}
