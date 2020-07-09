package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
    boolean parent = false;

    @Builder.Default
    boolean bad = false;

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
        if (regionData == null || !languages.isEmpty()) {
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

}
