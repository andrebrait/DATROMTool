package io.github.datromtool.io;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.ByteSize;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.domain.datafile.Datafile;
import io.github.datromtool.domain.datafile.Game;
import io.github.datromtool.domain.datafile.Rom;
import io.github.datromtool.domain.detector.BinaryTest;
import io.github.datromtool.domain.detector.Detector;
import io.github.datromtool.domain.detector.Rule;
import io.github.datromtool.domain.detector.enumerations.BinaryOperation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static java.util.Objects.requireNonNull;

@Slf4j
@Value
@AllArgsConstructor(access = AccessLevel.NONE)
class FileScannerParameters {

    public static final int MAX_BUFFER_NO_WARNING = 64 * 1024 * 1024; // 64MB

    int bufferSize;
    long minRomSize;
    long maxRomSize;
    boolean useLazyDetector;
    ImmutableSet<ArchiveType> alsoScanArchives;
    String minRomSizeStr;
    String maxRomSizeStr;

    private FileScannerParameters(
            int bufferSize,
            long minRomSize,
            long maxRomSize,
            boolean useLazyDetector,
            @Nonnull ImmutableSet<ArchiveType> alsoScanArchives) {
        this.bufferSize = bufferSize;
        this.minRomSize = minRomSize;
        this.maxRomSize = maxRomSize;
        this.minRomSizeStr = ByteSize.fromBytes(minRomSize).toFormattedString();
        this.maxRomSizeStr = ByteSize.fromBytes(maxRomSize).toFormattedString();
        this.useLazyDetector = useLazyDetector;
        this.alsoScanArchives = requireNonNull(alsoScanArchives);
    }

    public static FileScannerParameters withDefaults() {
        return new FileScannerParameters(
                32 * 1024, // 32KB
                0,
                Long.MAX_VALUE,
                false,
                ImmutableSet.of());
    }

    public static FileScannerParameters forDatWithDetector(
            @Nonnull AppConfig.FileScannerConfig config,
            @Nonnull Collection<Datafile> datafiles,
            @Nonnull Collection<Detector> detectors) {
        final int bufferSize;
        final long maxRomSize;
        final boolean useLazyDetector;
        final ImmutableSet<ArchiveType> alsoScanArchives = toRomStream(datafiles)
                .map(Rom::getName)
                .map(ArchiveType::parse)
                .filter(Objects::nonNull)
                .collect(ImmutableSet.toImmutableSet());
        final long minRomSize = toRomStream(datafiles)
                .mapToLong(Rom::getSize)
                .min()
                .orElse(0);
        if (detectors.isEmpty()) {
            bufferSize = config.getDefaultBufferSize();
            useLazyDetector = false;
            maxRomSize = toRomStream(datafiles)
                    .mapToLong(Rom::getSize)
                    .max()
                    .orElse(Long.MAX_VALUE);
        } else {
            long maxStartOffset = toRuleStream(detectors)
                    .mapToLong(Rule::getStartOffset)
                    .max()
                    .orElse(0);
            long minEndOffset = toRuleStream(detectors)
                    .mapToLong(Rule::getEndOffset)
                    .min()
                    .orElse(Long.MAX_VALUE);
            long maxUnheaderedSize = toRomStream(datafiles)
                    .mapToLong(Rom::getSize)
                    .max()
                    .orElse(Long.MAX_VALUE);
            if (maxStartOffset < 0) {
                maxStartOffset += maxUnheaderedSize;
            }
            if (minEndOffset < 0) {
                minEndOffset += maxUnheaderedSize;
            }
            maxStartOffset = max(maxStartOffset, 0);
            minEndOffset = min(minEndOffset, maxUnheaderedSize);
            maxRomSize = maxUnheaderedSize + maxStartOffset + (maxUnheaderedSize - minEndOffset);
            if (toRuleStream(detectors)
                    .map(Rule::getOperation)
                    .allMatch(BinaryOperation.NONE::equals)) {
                long minTestOffset = toRuleStream(detectors)
                        .flatMap(Rule::getAllBinaryTest)
                        .mapToLong(BinaryTest::getOffset)
                        .min()
                        .orElse(0);
                long minInitialOffset = toRuleStream(detectors)
                        .mapToLong(Rule::getStartOffset)
                        .min()
                        .orElse(0);
                if (minTestOffset >= 0 && minInitialOffset >= 0) {
                    long maxTestOffset = toRuleStream(detectors)
                            .flatMap(Rule::getAllBinaryTest)
                            .mapToLong(t -> t.getOffset() + t.getValue().length)
                            .max()
                            .orElse(0);
                    useLazyDetector = max(maxTestOffset, maxStartOffset) <= config.getDefaultBufferSize();
                } else {
                    useLazyDetector = false;
                }
            } else {
                useLazyDetector = false;
            }
            if (useLazyDetector) {
                bufferSize = config.getDefaultBufferSize();
            } else {
                bufferSize = toIntExact(max(min(maxRomSize, config.getMaxBufferSize()), config.getDefaultBufferSize()));
            }
            String bufferSizeStr = ByteSize.fromBytes(bufferSize).toFormattedString();
            if (bufferSize > MAX_BUFFER_NO_WARNING) {
                log.warn("Using a bigger I/O buffer size of {} due to header detection", bufferSizeStr);
            }
            if (bufferSize == config.getMaxBufferSize()) {
                log.warn("Disabling header detection for ROMs larger than {}", bufferSizeStr);
            }
            log.info("Using I/O buffer size of {}", bufferSizeStr);
        }
        return new FileScannerParameters(
                bufferSize,
                minRomSize,
                maxRomSize,
                useLazyDetector,
                alsoScanArchives);
    }

    private static Stream<Rom> toRomStream(@Nonnull Collection<Datafile> datafiles) {
        return datafiles.stream()
                .map(Datafile::getGames)
                .flatMap(Collection::stream)
                .map(Game::getRoms)
                .flatMap(Collection::stream);
    }

    private static Stream<Rule> toRuleStream(@Nonnull Collection<Detector> detectors) {
        return detectors.stream()
                .map(Detector::getRules)
                .flatMap(Collection::stream);
    }

}
