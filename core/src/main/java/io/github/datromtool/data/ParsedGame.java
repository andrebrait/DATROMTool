package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.RegionData.RegionDataEntry;
import io.github.datromtool.domain.datafile.logiqx.Game;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.google.common.base.Strings.isNullOrEmpty;
import static lombok.AccessLevel.PRIVATE;

@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
public class ParsedGame {

    @NonNull
    Game game;

    @NonNull
    RegionData regionData;

    @Builder.Default
    boolean parent = false;

    @Builder.Default
    boolean bad = false;

    @Builder.Default
    boolean bios = false;

    @NonNull
    @Builder.Default
    ImmutableSet<String> languages = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableList<Long> proto = ImmutableList.of();

    @NonNull
    @Builder.Default
    ImmutableList<Long> beta = ImmutableList.of();

    @NonNull
    @Builder.Default
    ImmutableList<Long> demo = ImmutableList.of();

    @NonNull
    @Builder.Default
    ImmutableList<Long> sample = ImmutableList.of();

    @NonNull
    @Builder.Default
    ImmutableList<Long> revision = ImmutableList.of();

    @NonNull
    @Builder.Default
    ImmutableList<Long> version = ImmutableList.of();

    @NonNull
    @Builder.Default
    ImmutableList<Divergence> divergences = ImmutableList.of();

    /**
     * Retool clone list group assignment (issue #19 step 2), set by
     * {@code io.github.datromtool.retool.CloneListMatcher} when this game's name matched a
     * variant title. {@code null} when no clone list is in play, or the clone list matched no
     * title for this game. {@code GameSorter} groups by this instead of {@link #getParentName()}
     * when present, letting a clone list unify games the DAT's own parent/clone data does not
     * (e.g. cross-region renames).
     */
    String clonelistGroup;

    /**
     * Effective priority from a clone list match (issue #19 step 2) - lower is preferred,
     * matching upstream's "1 is highest priority" convention. {@code null} when no clone list
     * assigned one (no clone list in play, or this game matched no title). Read by
     * {@code io.github.datromtool.sorting.PrioritySubComparator}, which stays neutral whenever
     * either compared game has a {@code null} priority here.
     */
    Integer clonelistPriority;

    @JsonInclude(NON_DEFAULT)
    public record Divergence(
            @Nonnull String field,
            @Nonnull ImmutableSet<String> detected,
            @Nonnull ImmutableSet<String> provided) {

        public Divergence {
            if (detected == null) detected = ImmutableSet.of();
            if (provided == null) provided = ImmutableSet.of();
        }
    }

    @JsonIgnore
    public Stream<String> getRegionsStream() {
        return regionData.regions().stream()
                .map(RegionDataEntry::code);
    }

    @JsonIgnore
    public Stream<String> getLanguagesStream() {
        if (!languages.isEmpty()) {
            return languages.stream();
        }
        return regionData.regions().stream()
                .map(RegionDataEntry::languages)
                .flatMap(Collection::stream);
    }

    @JsonIgnore
    public boolean isPrerelease() {
        return !proto.isEmpty()
                || !beta.isEmpty()
                || !demo.isEmpty()
                || !sample.isEmpty();
    }

    @JsonIgnore
    public String getParentName() {
        if (parent) {
            return game.getName();
        }
        if (!isNullOrEmpty(game.getCloneOf())) {
            return getGame().getCloneOf();
        }
        return getGame().getRomOf();
    }

}
