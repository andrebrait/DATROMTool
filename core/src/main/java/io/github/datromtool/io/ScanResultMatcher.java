package io.github.datromtool.io;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.datromtool.data.CrcKey;
import io.github.datromtool.data.Pair;
import io.github.datromtool.data.ParsedGame;
import io.github.datromtool.domain.datafile.Rom;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public final class ScanResultMatcher {

    private static final Comparator<FileScanner.Result> ARCHIVE_TYPE_COMPARATOR =
            Comparator.comparing(FileScanner.Result::getArchiveType)
                    .thenComparing(FileScanner.Result::getPath);

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Match {

        @NonNull
        Rom rom;
        @NonNull
        FileScanner.Result result;
    }

    @Value
    private static class MatchSet {

        @NonNull
        Rom rom;
        @NonNull
        ImmutableList<FileScanner.Result> results;
    }

    private final ImmutableMap<CrcKey, ImmutableList<FileScanner.Result>> resultsForCrc;
    private final ImmutableMap<String, ImmutableList<FileScanner.Result>> resultsForMd5;
    private final ImmutableMap<String, ImmutableList<FileScanner.Result>> resultsForSha1;

    public ScanResultMatcher(Collection<FileScanner.Result> results) {
        this.resultsForCrc = toSortedMap(results, CrcKey::from);
        this.resultsForMd5 = toSortedMap(results, r -> r.getDigest().getMd5());
        this.resultsForSha1 = toSortedMap(results, r -> r.getDigest().getSha1());
    }

    private static <L> ImmutableMap<L, ImmutableList<FileScanner.Result>> toSortedMap(
            Collection<FileScanner.Result> results,
            Function<FileScanner.Result, L> function) {
        return results.stream()
                .collect(Collectors.groupingBy(
                        function,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .entrySet()
                .stream()
                .filter(e -> !e.getValue().isEmpty())
                .peek(e -> e.getValue().sort(ARCHIVE_TYPE_COMPARATOR))
                .collect(ImmutableMap.toImmutableMap(
                        Map.Entry::getKey,
                        e -> ImmutableList.copyOf(e.getValue())));
    }

    @Nonnull
    private ImmutableList<FileScanner.Result> match(Rom rom) {
        ImmutableList<FileScanner.Result> results = null;
        if (rom.getSha1() != null) {
            results = resultsForSha1.get(rom.getSha1());
        }
        if (results == null && rom.getMd5() != null) {
            results = resultsForMd5.get(rom.getMd5());
        }
        if (results == null && rom.getSize() != null && rom.getCrc() != null) {
            results = resultsForCrc.get(CrcKey.from(rom));
        }
        if (results == null) {
            results = ImmutableList.of();
        }
        if (results.isEmpty()) {
            log.warn("Missing ROM file: '{}'", rom.getName());
        }
        return results;
    }

    @Nonnull
    public ImmutableList<Match> match(
            @Nonnull ParsedGame parsedGame,
            @Nullable ArchiveType archiveType) {
        ImmutableList<MatchSet> matchSets = parsedGame.getGame().getRoms().stream()
                .map(r -> new MatchSet(r, match(r)))
                .filter(s -> !s.getResults().isEmpty())
                .collect(ImmutableList.toImmutableList());
        if (matchSets.size() < parsedGame.getGame().getRoms().size()) {
            log.warn("Skipping '{}' due to missing files", parsedGame.getGame().getName());
            return ImmutableList.of();
        }
        ImmutableList<Match> uncompressedMatches = matchSets.stream()
                .map(s -> s.getResults().stream()
                        .filter(r -> r.getArchiveType() == null)
                        .findFirst()
                        .map(o -> new Match(s.getRom(), o)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(ImmutableList.toImmutableList());
        if (uncompressedMatches.size() >= parsedGame.getGame().getRoms().size()) {
            return uncompressedMatches;
        }
        ImmutableMap<Path, ImmutableList<Match>> matchesPerArchive = matchSets.stream()
                .flatMap(s -> s.getResults()
                        .stream()
                        .filter(r -> r.getArchiveType() != null)
                        .map(r -> Pair.of(r.getPath(), new Match(s.getRom(), r))))
                .collect(Collectors.groupingBy(
                        Pair::getLeft,
                        LinkedHashMap::new,
                        Collectors.mapping(Pair::getRight, ImmutableList.toImmutableList())))
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(e -> -e.getValue().size()))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        if (archiveType == null) {
            Iterator<Match> remaining = matchesPerArchive.values().stream()
                    .flatMap(Collection::stream)
                    .filter(m -> !uncompressedMatches.contains(m))
                    .iterator();
            return ImmutableList.<Match>builder()
                    .addAll(uncompressedMatches)
                    .addAll(remaining)
                    .build();
        }
        return matchesPerArchive.values().stream()
                .filter(l -> l.size() >= parsedGame.getGame().getRoms().size())
                .findFirst()
                .orElseGet(() -> {
                    log.warn(
                            "Could not find all ROMs for '{}' in a single archive",
                            parsedGame.getGame().getName());
                    return ImmutableList.of();
                });
    }

}
