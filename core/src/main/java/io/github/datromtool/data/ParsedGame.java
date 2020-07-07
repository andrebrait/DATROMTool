package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import io.github.datromtool.data.RegionData.RegionDataEntry;
import io.github.datromtool.domain.datafile.Game;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Collection;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
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
    boolean isParent = false;

    @Builder.Default
    boolean isBad = false;

    @NonNull
    @Builder.Default
    ImmutableSet<String> languages = ImmutableSet.of();

    @NonNull
    @Builder.Default
    ImmutableList<Integer> proto = ImmutableList.of();

    @NonNull
    @Builder.Default
    ImmutableList<Integer> beta = ImmutableList.of();

    @NonNull
    @Builder.Default
    ImmutableList<Integer> demo = ImmutableList.of();

    @NonNull
    @Builder.Default
    ImmutableList<Integer> sample = ImmutableList.of();

    @NonNull
    @Builder.Default
    ImmutableList<Integer> revision = ImmutableList.of();

    @NonNull
    @Builder.Default
    ImmutableList<Integer> version = ImmutableList.of();

    @JsonIgnore
    public Stream<String> getRegionsStream() {
        if (regionData == null) {
            return Stream.empty();
        }
        return regionData.getRegions().stream()
                .map(RegionDataEntry::getCode);
    }

    @JsonIgnore
    public Stream<String> getLanguagesStream() {
        if (regionData == null) {
            return languages.stream();
        }
        return Streams.concat(
                languages.stream(),
                regionData.getRegions().stream()
                        .map(RegionDataEntry::getLanguages)
                        .flatMap(Collection::stream));
    }

}
