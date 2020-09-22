package io.github.datromtool.io;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.ByteUnit;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.toIntExact;

@Value
@AllArgsConstructor(access = AccessLevel.NONE)
class FileScannerParameters {

    private static final Logger logger = LoggerFactory.getLogger(FileScannerParameters.class);

    public static final int DEFAULT_BUFFER_SIZE = 32 * 1024; // 32KB
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
            ImmutableSet<ArchiveType> alsoScanArchives) {
        this.bufferSize = bufferSize;
        this.minRomSize = minRomSize;
        this.maxRomSize = maxRomSize;
        this.minRomSizeStr = makeRomSizeStr(minRomSize);
        this.maxRomSizeStr = makeRomSizeStr(maxRomSize);
        this.useLazyDetector = useLazyDetector;
        this.alsoScanArchives = alsoScanArchives;
    }

    private static String makeRomSizeStr(long size) {
        ByteUnit minRomSizeUnit = ByteUnit.getUnit(size);
        return String.format("%.02f %s", minRomSizeUnit.convert(size), minRomSizeUnit.getSymbol());
    }

    public static FileScannerParameters withDefaults() {
        return new FileScannerParameters(
                DEFAULT_BUFFER_SIZE,
                0,
                Long.MAX_VALUE,
                false,
                ImmutableSet.of());
    }

    public static FileScannerParameters forDatWithDetector(
            @Nonnull AppConfig appConfig,
            @Nonnull List<Datafile> datafiles,
            @Nonnull List<Detector> detectors) {
        final int maxBuffer = appConfig.getScanner().getMaxBufferSize();
        final int bufferSize;
        final long maxRomSize;
        final boolean useLazyDetector;
        final ImmutableSet<ArchiveType> alsoScanArchives = datafiles.stream()
                .map(Datafile::getGames)
                .flatMap(Collection::stream)
                .map(Game::getRoms)
                .flatMap(Collection::stream)
                .filter(r -> r.getSize() != null)
                .map(Rom::getName)
                .map(ArchiveType::parse)
                .filter(at -> at != ArchiveType.NONE)
                .collect(ImmutableSet.toImmutableSet());
        final long minRomSize = datafiles.stream()
                .map(Datafile::getGames)
                .flatMap(Collection::stream)
                .map(Game::getRoms)
                .flatMap(Collection::stream)
                .filter(r -> r.getSize() != null)
                .mapToLong(Rom::getSize)
                .min()
                .orElse(0);
        if (detectors.isEmpty()) {
            bufferSize = DEFAULT_BUFFER_SIZE;
            useLazyDetector = false;
            maxRomSize = datafiles.stream()
                    .map(Datafile::getGames)
                    .flatMap(Collection::stream)
                    .map(Game::getRoms)
                    .flatMap(Collection::stream)
                    .filter(r -> r.getSize() != null)
                    .mapToLong(Rom::getSize)
                    .max()
                    .orElse(Long.MAX_VALUE);
        } else {
            long maxStartOffset = detectors.stream()
                    .map(Detector::getRules)
                    .flatMap(Collection::stream)
                    .filter(r -> r.getStartOffset() != null)
                    .mapToLong(Rule::getStartOffset)
                    .max()
                    .orElse(0);
            long minEndOffset = detectors.stream()
                    .map(Detector::getRules)
                    .flatMap(Collection::stream)
                    .filter(r -> r.getEndOffset() != null)
                    .mapToLong(Rule::getEndOffset)
                    .min()
                    .orElse(Long.MAX_VALUE);
            long maxUnheaderedSize = datafiles.stream()
                    .map(Datafile::getGames)
                    .flatMap(Collection::stream)
                    .map(Game::getRoms)
                    .flatMap(Collection::stream)
                    .filter(r -> r.getSize() != null)
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
            if (detectors.stream()
                    .map(Detector::getRules)
                    .flatMap(Collection::stream)
                    .map(Rule::getOperation)
                    .allMatch(BinaryOperation.NONE::equals)) {
                long minTestOffset = detectors.stream()
                        .map(Detector::getRules)
                        .flatMap(Collection::stream)
                        .flatMap(Rule::getAllBinaryTest)
                        .mapToLong(BinaryTest::getOffset)
                        .min()
                        .orElse(0);
                long minInitialOffset = detectors.stream()
                        .map(Detector::getRules)
                        .flatMap(Collection::stream)
                        .mapToLong(Rule::getStartOffset)
                        .min()
                        .orElse(0);
                if (minTestOffset >= 0 && minInitialOffset >= 0) {
                    long maxTestOffset = detectors.stream()
                            .map(Detector::getRules)
                            .flatMap(Collection::stream)
                            .flatMap(Rule::getAllBinaryTest)
                            .mapToLong(t -> t.getOffset() + t.getValue().length)
                            .max()
                            .orElse(0);
                    useLazyDetector = max(maxTestOffset, maxStartOffset) <= DEFAULT_BUFFER_SIZE;
                } else {
                    useLazyDetector = false;
                }
            } else {
                useLazyDetector = false;
            }
            if (useLazyDetector) {
                bufferSize = DEFAULT_BUFFER_SIZE;
            } else {
                bufferSize = toIntExact(max(min(maxRomSize, maxBuffer), DEFAULT_BUFFER_SIZE));
            }
            ByteUnit unit = ByteUnit.getUnit(bufferSize);
            String bufferSizeStr = String.format("%.02f", unit.convert(bufferSize));
            if (bufferSize > MAX_BUFFER_NO_WARNING) {
                logger.warn(
                        "Using a bigger I/O buffer size of {} {} due to header detection",
                        bufferSizeStr,
                        unit.getSymbol());
                if (bufferSize == maxBuffer) {
                    logger.warn(
                            "Disabling header detection for ROMs larger than {} {}",
                            bufferSizeStr,
                            unit.getSymbol());
                }
            } else {
                logger.info("Using I/O buffer size of {} {}", bufferSizeStr, unit.getSymbol());
            }
        }
        return new FileScannerParameters(
                bufferSize,
                minRomSize,
                maxRomSize,
                useLazyDetector,
                alsoScanArchives);
    }

}
