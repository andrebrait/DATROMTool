package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.datromtool.domain.datafile.logiqx.Rom;
import io.github.datromtool.io.FileScanner;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record CrcKey(@Nonnull Long size, @Nonnull String crc) {

    public static CrcKey of(@Nonnull Long size, @Nonnull String crc) {
        return new CrcKey(size, crc);
    }

    @Nonnull
    public static CrcKey from(@Nonnull Rom rom) {
        return of(rom.size(), rom.crc());
    }

    @Nonnull
    public static CrcKey from(@Nonnull FileScanner.Result result) {
        return of(result.getUnheaderedSize(), result.getDigest().getCrc());
    }
}
