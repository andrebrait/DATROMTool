package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.RegionData.RegionDataEntry;
import io.github.datromtool.domain.datafile.Game;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

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

    @JsonIgnore
    public Stream<String> getRegionsStream() {
        return regionData.getRegions().stream()
                .map(RegionDataEntry::getCode);
    }

    @JsonIgnore
    public Stream<String> getLanguagesStream() {
        if (!languages.isEmpty()) {
            return languages.stream();
        }
        return regionData.getRegions().stream()
                .map(RegionDataEntry::getLanguages)
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
