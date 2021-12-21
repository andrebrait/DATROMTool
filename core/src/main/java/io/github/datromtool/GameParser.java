package io.github.datromtool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.datafile.*;
import io.github.datromtool.domain.datafile.enumerations.Status;
import io.github.datromtool.domain.datafile.enumerations.YesNo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Strings.isNullOrEmpty;

@Slf4j
@RequiredArgsConstructor
public final class GameParser {

    private final static Pattern COMMA_OR_PLUS = Pattern.compile("[,+]");
    private final static Pattern NUMERIC_HEX =
            Pattern.compile("^-?[a-f0-9]+$", Pattern.CASE_INSENSITIVE);

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
                        .parent(isNullOrEmpty(g.getCloneOf()) && isNullOrEmpty(g.getRomOf()))
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
        boolean result = g.getIsBios() == YesNo.YES || Patterns.BIOS.matcher(g.getName()).find();
        if (result) {
            log.debug("'{}' detected as BIOS", g.getName());
        }
        return result;
    }

    private static boolean detectIsBad(Game g) {
        boolean result = Patterns.BAD.matcher(g.getName()).find()
                || g.getRoms().stream().map(Rom::getStatus).anyMatch(Status.BAD_DUMP::equals)
                || g.getDisks().stream().map(Disk::getStatus).anyMatch(Status.BAD_DUMP::equals);
        if (result) {
            log.debug("'{}' detected as Bad Dump", g.getName());
        }
        return result;
    }

    private RegionData detectRegionData(Game game) {
        Set<RegionData.RegionDataEntry> detected = new LinkedHashSet<>();
        Matcher matcher = Patterns.SECTIONS.matcher(game.getName());
        while (matcher.find()) {
            for (String element : matcher.group(1).split(",")) {
                for (RegionData.RegionDataEntry regionDataEntry : regionData.getRegions()) {
                    if (regionDataEntry.getPattern().matcher(element.trim()).matches()) {
                        log.debug(
                                "Detected region '{}' for '{}'",
                                regionDataEntry.getCode(),
                                game.getName());
                        detected.add(regionDataEntry);
                    }
                }
            }
        }
        Set<RegionData.RegionDataEntry> provided = new LinkedHashSet<>();
        for (Release release : game.getReleases()) {
            if (!release.getRegion().isEmpty()) {
                String code = release.getRegion().trim().toUpperCase();
                RegionData.RegionDataEntry regionDataEntry = regionData.getRegions().stream()
                        .filter(e -> e.getCode().equals(code))
                        .findFirst()
                        .orElseGet(() -> {
                            log.warn("Unrecognized region: '{}' in {}", code, release);
                            return RegionData.RegionDataEntry.builder().code(code).build();
                        });
                log.debug(
                        "DAT provided region '{}' for '{}'",
                        regionDataEntry.getCode(),
                        game.getName());
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
            log.warn(
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
            for (String part : COMMA_OR_PLUS.split(matcher.group(1))) {
                String language = part.trim().toLowerCase();
                if (!language.isEmpty()) {
                    log.debug(
                            "Detected language '{}' for '{}'",
                            language,
                            game.getName());
                    detected.add(language);
                }
            }
        }
        Set<String> provided = new LinkedHashSet<>();
        for (Release release : game.getReleases()) {
            if (release.getLanguage() != null && !release.getLanguage().isEmpty()) {
                for (String part : COMMA_OR_PLUS.split(release.getLanguage())) {
                    String language = part.trim().toLowerCase();
                    if (!language.isEmpty()) {
                        log.debug(
                                "DAT provided language '{}' for '{}'",
                                language,
                                game.getName());
                        provided.add(language);
                    }
                }
            }
        }
        if (shouldLogDivergences(detected, provided)) {
            log.warn(
                    "Detected languages by name do not match with the ones provided by the DAT. "
                            + "Difference(detected={}, provided={}, game={})",
                    detected,
                    provided,
                    game.getName());
        }
        return ImmutableSet.<String>builder()
                .addAll(detected)
                .addAll(provided)
                .build();
    }

    private boolean shouldLogDivergences(Set<?> detected, Set<?> provided) {
        return detection != DivergenceDetection.IGNORE
                && ((detection == DivergenceDetection.ALWAYS && !provided.equals(detected))
                || (!detected.isEmpty() && !provided.isEmpty()
                && ((detection == DivergenceDetection.ONE_WAY && !provided.containsAll(detected))
                || (detection == DivergenceDetection.TWO_WAY && !provided.equals(detected)))));
    }

    private static ImmutableList<Long> detectProto(Game game) {
        ImmutableList<Long> longs = parseNumberFromPattern(Patterns.PROTO, game);
        if (!longs.isEmpty()) {
            log.debug("Detected Proto {} for '{}'", longs, game.getName());
        }
        return longs;
    }

    private static ImmutableList<Long> detectBeta(Game game) {
        ImmutableList<Long> longs = parseNumberFromPattern(Patterns.BETA, game);
        if (!longs.isEmpty()) {
            log.debug("Detected Beta {} for '{}'", longs, game.getName());
        }
        return longs;
    }

    private static ImmutableList<Long> detectDemo(Game game) {
        ImmutableList<Long> longs = parseNumberFromPattern(Patterns.DEMO, game);
        if (!longs.isEmpty()) {
            log.debug("Detected Demo {} for '{}'", longs, game.getName());
        }
        return longs;
    }

    private static ImmutableList<Long> detectSample(Game game) {
        ImmutableList<Long> longs = parseNumberFromPattern(Patterns.SAMPLE, game);
        if (!longs.isEmpty()) {
            log.debug("Detected Sample {} for '{}'", longs, game.getName());
        }
        return longs;
    }

    private static ImmutableList<Long> detectRevision(Game game) {
        ImmutableList<Long> longs = parseNumberFromPattern(Patterns.REVISION, game);
        if (!longs.isEmpty()) {
            log.debug("Detected Revision {} for '{}'", longs, game.getName());
        }
        return longs;
    }

    private static ImmutableList<Long> detectVersion(Game game) {
        ImmutableList<Long> longs = parseNumberFromPattern(Patterns.VERSION, game);
        if (!longs.isEmpty()) {
            log.debug("Detected Version {} for '{}'", longs, game.getName());
        }
        return longs;
    }

    private static ImmutableList<Long> parseNumberFromPattern(Pattern pattern, Game game) {
        Matcher matcher = pattern.matcher(game.getName());
        if (matcher.find()) {
            return IntStream.range(1, matcher.groupCount() + 1)
                    .filter(i -> i == 1 || matcher.group(i) != null)
                    .mapToObj(matcher::group)
                    .map(s -> s == null ? "" : s)
                    .map(s -> s.split("\\."))
                    .flatMap(Arrays::stream)
                    .map(n -> {
                        if (NUMERIC_HEX.matcher(n).matches()) {
                            return Long.valueOf(n, 16);
                        }
                        return n.toLowerCase().chars()
                                .mapToObj(Long::valueOf)
                                .reduce(0L, (i, j) -> (i * 16) + j);
                    }).collect(ImmutableList.toImmutableList());
        }
        return ImmutableList.of();
    }

}
