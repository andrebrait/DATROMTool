package io.github.datromtool;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.data.RegionData.RegionDataEntry;
import io.github.datromtool.generated.datafile.Game;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Collection;

@Value
@Builder
public class ParsedGame {

    @NonNull
    Game game;
    @NonNull
    RegionData regionData;
    @NonNull
    ImmutableSet<String> languages;

    // TODO: add version and region information

    public boolean containsLanguage(String language) {
        return languages.contains(language) || isInRegionData(language);
    }

    public boolean containsAnyLanguage(Collection<String> languages) {
        return languages.stream().anyMatch(this::containsLanguage);
    }

    private boolean isInRegionData(String language) {
        return regionData.getRegions().stream()
                .map(RegionDataEntry::getLanguages)
                .flatMap(Collection::stream)
                .anyMatch(language::equals);
    }

    public boolean containsRegion(String code) {
        return regionData.getRegions().stream()
                .map(RegionDataEntry::getCode)
                .anyMatch(code::equals);
    }

    public boolean containsAnyRegion(Collection<String> codes) {
        return codes.stream().anyMatch(this::containsRegion);
    }

}
