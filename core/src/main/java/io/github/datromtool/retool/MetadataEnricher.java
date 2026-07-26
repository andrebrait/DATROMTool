package io.github.datromtool.retool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.domain.retool.MetadataEntry;
import io.github.datromtool.domain.retool.RetoolMetadata;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Locale;

import static lombok.AccessLevel.PRIVATE;

/**
 * Supplements {@link ParsedGame#getLanguages()} from a {@link RetoolMetadata} file (issue #19
 * step 2) when name-parsing (and DAT-provided release data) found none - e.g. a DAT/No-Intro name
 * with no {@code (En,Fr,...)} language tag. Upstream keys metadata entries by a title's exact
 * full name (see {@link RetoolMetadata}'s Javadoc), so lookup here is an exact
 * {@link io.github.datromtool.domain.datafile.logiqx.Game#getName()} match - no fuzzy/short-name
 * matching like {@link CloneListMatcher}.
 *
 * <p>Metadata language codes are titlecase (e.g. {@code "En"}, {@code "Fr"} - see the real
 * fixture backing {@code RetoolMetadataTest}); {@link ParsedGame#getLanguages()} elsewhere in
 * this codebase is always lowercase ({@code GameParser#detectLanguages} lowercases every
 * detected/DAT-provided code). Codes are lowercased here to match that convention, so
 * {@code GameFilterer}'s language include/exclude filters - which compare against lowercase
 * {@code --include-languages}/{@code --exclude-languages} values - see an enriched game exactly
 * as they would a name-parsed one.
 *
 * <p>Absent metadata ({@code null} or empty), {@link #enrich} is a no-op copy: no clone list/
 * metadata input sources means byte-identical behavior to before issue #19.
 */
@NoArgsConstructor(access = PRIVATE)
public final class MetadataEnricher {

    @Nonnull
    public static ImmutableList<ParsedGame> enrich(
            @Nonnull Collection<ParsedGame> games,
            @Nullable RetoolMetadata metadata) {
        if (metadata == null || metadata.entries().isEmpty()) {
            return ImmutableList.copyOf(games);
        }
        ImmutableList.Builder<ParsedGame> builder = ImmutableList.builder();
        for (ParsedGame game : games) {
            builder.add(enrichOne(game, metadata));
        }
        return builder.build();
    }

    private static ParsedGame enrichOne(ParsedGame game, RetoolMetadata metadata) {
        if (!game.getLanguages().isEmpty()) {
            return game;
        }
        MetadataEntry entry = metadata.entries().get(game.getGame().getName());
        if (entry == null || entry.languages().isEmpty()) {
            return game;
        }
        ImmutableSet<String> languages = entry.languages().stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(ImmutableSet.toImmutableSet());
        return game.toBuilder().languages(languages).build();
    }
}
