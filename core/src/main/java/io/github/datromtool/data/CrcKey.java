package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.datromtool.domain.datafile.Rom;
import io.github.datromtool.io.FileScanner;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@AllArgsConstructor(staticName = "from")
@JsonInclude(NON_NULL)
public class CrcKey {

    @NonNull
    Long size;
    @NonNull
    String crc;

    @Nonnull
    public static CrcKey from(@Nonnull Rom rom) {
        return from(rom.getSize(), rom.getCrc());
    }

    @Nonnull
    public static CrcKey from(@Nonnull FileScanner.Result result) {
        return from(result.getUnheaderedSize(), result.getDigest().getCrc());
    }
}
