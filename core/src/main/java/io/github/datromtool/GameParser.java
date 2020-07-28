package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.datafile.Disk;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.domain.datafile.Release;
import io.github.datromtool.domain.datafile.Rom;
import io.github.datromtool.domain.datafile.enumerations.Status;
import io.github.datromtool.domain.datafile.enumerations.YesNo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@RequiredArgsConstructor
public final class GameParser {

    private final static Logger logger = LoggerFactory.getLogger(GameParser.class);
    private final static Pattern COMMA_OR_PLUS = Pattern.compile("[,+]");

    public enum DivergenceDetection {
        IGNORE,
        ONE_WAY,
        TWO_WAY,
        ALWAYS
    }

    @NonNull
    private final RegionData regionData;

    @NonNull
    private final GameParser.DivergenceDetection detection;

    public ImmutableList<ParsedGame> parse(Datafile input) {
        return input.getGames().stream()
                .map(g -> ParsedGame.builder()
                        .game(g)
                        .bios(isBios(g))
                        .parent(isBlank(g.getCloneOf()) && isBlank(g.getRomOf()))
                        .bad(detectIsBad(g))
                        .regionData(detectRegionData(g))
                        .languages(detectLanguages(g))
                        .proto(detectProto(g))
                        .beta(detectBeta(g))
                        .demo(detectDemo(g))
                        .sample(detectSample(g))
                        .revision(detectRevision(g))
                        .version(detectVersion(g))
                        .build())
                .collect(ImmutableList.toImmutableList());
    }

    private static boolean isBios(Game g) {
        return g.getIsBios() == YesNo.YES || Patterns.BIOS.matcher(g.getName()).find();
    }

    private static boolean detectIsBad(Game g) {
        return Patterns.BAD.matcher(g.getName()).find()
                || g.getRoms().stream().map(Rom::getStatus).anyMatch(Status.BAD_DUMP::equals)
                || g.getDisks().stream().map(Disk::getStatus).anyMatch(Status.BAD_DUMP::equals);
    }

    private RegionData detectRegionData(Game game) {
        Set<RegionData.RegionDataEntry> detected = new LinkedHashSet<>();
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
        Set<RegionData.RegionDataEntry> provided = new LinkedHashSet<>();
        for (Release release : game.getReleases()) {
            if (isNotBlank(release.getRegion())) {
                String code = release.getRegion().trim().toUpperCase();
                RegionData.RegionDataEntry regionDataEntry = regionData.getRegions().stream()
                        .filter(e -> e.getCode().equals(code))
                        .findFirst()
                        .orElseGet(() -> {
                            logger.warn("Unrecognized region: '{}' in {}", code, release);
                            return RegionData.RegionDataEntry.builder().code(code).build();
                        });
                provided.add(regionDataEntry);
            }
        }
        if (shouldLogDivergences(detected, provided)) {
            List<String> detectedCodes = detected.stream()
                    .map(RegionData.RegionDataEntry::getCode)
                    .collect(Collectors.toList());
            List<String> providedCodes = provided.stream()
                    .map(RegionData.RegionDataEntry::getCode)
                    .collect(Collectors.toList());
            logger.warn(
                    "Detected regions by name do not match with the ones provided by the DAT. "
                            + "Difference(detected={}, provided={}, game={})",
                    detectedCodes, providedCodes, game.getName());
        }
        return RegionData.builder()
                .regions(ImmutableSet.<RegionData.RegionDataEntry>builder()
                        .addAll(detected)
                        .addAll(provided)
                        .build())
                .build();
    }

    private ImmutableSet<String> detectLanguages(Game game) {
        Set<String> detected = new LinkedHashSet<>();
        Matcher matcher = Patterns.LANGUAGES.matcher(game.getName());
        while (matcher.find()) {
            for (String language : COMMA_OR_PLUS.split(matcher.group(1))) {
                if (isNotEmpty(language)) {
                    detected.add(language.toLowerCase());
                }
            }
        }
        Set<String> provided = new LinkedHashSet<>();
        for (Release release : game.getReleases()) {
            if (isNotBlank(release.getLanguage())) {
                for (String language : COMMA_OR_PLUS.split(release.getLanguage())) {
                    if (isNotBlank(language)) {
                        provided.add(language.trim().toLowerCase());
                    }
                }
            }
        }
        if (shouldLogDivergences(detected, provided)) {
            logger.warn(
                    "Detected languages by name do not match with the ones provided by the DAT. "
                            + "Difference(detected={}, provided={}, game={})",
                    detected, provided, game.getName());
        }
        return ImmutableSet.<String>builder()
                .addAll(detected)
                .addAll(provided)
                .build();
    }

    private boolean shouldLogDivergences(Set<?> detected, Set<?> provided) {
        return (detection == DivergenceDetection.ALWAYS && !provided.equals(detected))
                || (!detected.isEmpty() && !provided.isEmpty()
                && ((detection == DivergenceDetection.ONE_WAY && !provided.containsAll(detected))
                || (detection == DivergenceDetection.TWO_WAY && !provided.equals(detected))));
    }

    private static ImmutableList<Integer> detectProto(Game game) {
        return parseNumberFromPattern(Patterns.PROTO, game);
    }

    private static ImmutableList<Integer> detectBeta(Game game) {
        return parseNumberFromPattern(Patterns.BETA, game);
    }

    private static ImmutableList<Integer> detectDemo(Game game) {
        return parseNumberFromPattern(Patterns.DEMO, game);
    }

    private static ImmutableList<Integer> detectSample(Game game) {
        return parseNumberFromPattern(Patterns.SAMPLE, game);
    }

    private static ImmutableList<Integer> detectRevision(Game game) {
        return parseNumberFromPattern(Patterns.REVISION, game);
    }

    private static ImmutableList<Integer> detectVersion(Game game) {
        return parseNumberFromPattern(Patterns.VERSION, game);
    }

    private static ImmutableList<Integer> parseNumberFromPattern(Pattern pattern, Game game) {
        Matcher matcher = pattern.matcher(game.getName());
        if (matcher.find()) {
            return IntStream.range(1, matcher.groupCount() + 1)
                    .filter(i -> i == 1 || matcher.group(i) != null)
                    .mapToObj(matcher::group)
                    .map(StringUtils::defaultString)
                    .map(s -> s.split("\\."))
                    .flatMap(Arrays::stream)
                    .map(n -> {
                        try {
                            return Integer.valueOf(n, 16);
                        } catch (NumberFormatException ignore) {
                        }
                        return n.toLowerCase().chars()
                                .reduce(0, (i, j) -> (i * 16) + j);
                    }).collect(ImmutableList.toImmutableList());
        }
        return ImmutableList.of();
    }

}
