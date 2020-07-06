package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.domain.datafile.Release;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@RequiredArgsConstructor
public final class GameParser {

    private final static Logger logger = LoggerFactory.getLogger(GameParser.class);

    private final RegionData regionData;

    public ImmutableList<ParsedGame> parse(Datafile input) {
        return input.getGames().stream()
                .map(g -> ParsedGame.builder()
                        .game(g)
                        .regionData(detectRegionData(g))
                        .languages(detectLanguages(g))
                        .build())
                .collect(ImmutableList.toImmutableList());
    }

    private RegionData detectRegionData(Game game) {
        ImmutableSet.Builder<RegionData.RegionDataEntry> detected = new ImmutableSet.Builder<>();
        Matcher matcher = Patterns.SECTIONS.matcher(game.getName());
        while (matcher.find()) {
            for (String element : matcher.group(1).split(",")) {
                for (RegionData.RegionDataEntry regionDataEntry : regionData.getRegions()) {
                    if (regionDataEntry.getPattern().matcher(element.trim()).matches()) {
                        detected.add(regionDataEntry);
                    }
                }
            }
        }
        for (Release release : game.getReleases()) {
            String code = release.getRegion();
            if (isNotEmpty(code)) {
                detected.add(findRegionDataEntry(code.toUpperCase()));
            }
        }
        return RegionData.builder().regions(detected.build()).build();
    }

    private RegionData.RegionDataEntry findRegionDataEntry(String code) {
        return regionData.getRegions().stream()
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseGet(() -> {
                    logger.warn("Unrecognized region: '{}'", code);
                    return RegionData.RegionDataEntry.builder().code(code).build();
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
        for (Release release : game.getReleases()) {
            if (isNotEmpty(release.getLanguage())) {
                detected.add(release.getLanguage().toLowerCase());
            }
        }
        return detected.build();
    }

}
