package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.data.RegionData.RegionDataEntry;
import io.github.datromtool.generated.datafile.Datafile;
import io.github.datromtool.generated.datafile.Game;
import io.github.datromtool.generated.datafile.Release;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@RequiredArgsConstructor
public final class DatafileFilterer {

    private final static Logger logger = LoggerFactory.getLogger(DatafileFilterer.class);

    @SuppressWarnings("RegExpUnexpectedAnchor")
    private final static Pattern NO_MATCH = Pattern.compile("a^");

    @NonNull
    private final Filter filter;
    @NonNull
    private final RegionData regionData;

    public ImmutableList<ParsedGame> filter(Datafile input) {
        return input.getGame().stream()
                .map(g -> ParsedGame.builder()
                        .game(g)
                        .regionData(detectRegionData(g))
                        .languages(detectLanguages(g))
                        .build())
                .filter(p -> {
                    switch (filter.getMode()) {
                        case DEFAULT:
                            return p.containsAnyRegion(filter.getRegions());
                        case ONLY_WITH_LANG:
                            return p.containsAnyRegion(filter.getRegions())
                                    && p.containsAnyLanguage(filter.getLanguages());
                        case ALL_WITH_LANG:
                            return p.containsAnyLanguage(filter.getLanguages())
                                    || p.containsAnyRegion(filter.getRegions());
                        case ALL_REGIONS:
                        default:
                            return true;
                    }
                }).collect(ImmutableList.toImmutableList());
    }

    private RegionData detectRegionData(Game game) {
        ImmutableSet.Builder<RegionDataEntry> detected = new ImmutableSet.Builder<>();
        Matcher matcher = Patterns.SECTIONS.matcher(game.getName());
        while (matcher.find()) {
            for (String element : matcher.group(1).split(",")) {
                for (RegionDataEntry regionDataEntry : regionData.getRegions()) {
                    if (regionDataEntry.getPattern().matcher(element.trim()).matches()) {
                        detected.add(regionDataEntry);
                    }
                }
            }
        }
        for (Release release : game.getRelease()) {
            String code = release.getRegion();
            if (isNotEmpty(code)) {
                detected.add(findRegionDataEntry(code.toUpperCase()));
            }
        }
        return RegionData.builder()
                .regions(detected.build())
                .build();
    }

    private RegionDataEntry findRegionDataEntry(String code) {
        return regionData.getRegions().stream()
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseGet(() -> {
                    logger.warn("Unrecognized region: '{}'", code);
                    return RegionDataEntry.builder()
                            .code(code)
                            .pattern(NO_MATCH)
                            .languages(ImmutableSet.of())
                            .build();
                });
    }

    private ImmutableSet<String> detectLanguages(Game game) {
        ImmutableSet.Builder<String> detected = new ImmutableSet.Builder<>();
        Matcher matcher = Patterns.LANGUAGES.matcher(game.getName());
        while (matcher.find()) {
            for (String element : matcher.group(1).split(",")) {
                for (String language : element.split("\\+")) {
                    if (isNotEmpty(language)) {
                        detected.add(language.toLowerCase());
                    }
                }
            }
        }
        for (Release release : game.getRelease()) {
            if (isNotEmpty(release.getLanguage())) {
                detected.add(release.getLanguage().toLowerCase());
            }
        }
        return detected.build();
    }

}
