package io.github.datromtool.sorting;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.SortingPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Comparator;

public final class GameComparator implements Comparator<ParsedGame> {

    private final static Logger logger = LoggerFactory.getLogger(GameComparator.class);

    private final ImmutableList<SubComparator> subComparators;

    public GameComparator(@Nonnull SortingPreference sortingPreference) {
        this(sortingPreference, SubComparatorProvider.INSTANCE);
    }

    @VisibleForTesting
    GameComparator(
            @Nonnull SortingPreference sortingPreference,
            @Nonnull SubComparatorProvider provider) {
        subComparators = provider.toList(sortingPreference);
    }

    @Override
    public int compare(ParsedGame o1, ParsedGame o2) {
        for (SubComparator subComparator : subComparators) {
            int result = subComparator.compare(o1, o2);
            if (result != 0) {
                logger.debug(
                        "Under criteria '{}'{}, '{}' is preferred over '{}'",
                        subComparator.getCriteria(),
                        subComparator.isReversed() ? " (reversed)" : "",
                        result < 0 ? o1.getGame().getName() : o2.getGame().getName(),
                        result < 0 ? o2.getGame().getName() : o1.getGame().getName());
                return result;
            }
        }
        logger.debug(
                "'{}' and '{}' are equal under every criteria",
                o1.getGame().getName(),
                o2.getGame().getName());
        return 0;
    }
}
