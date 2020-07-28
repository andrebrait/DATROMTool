package io.github.datromtool.domain.detector.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NumberUtils {

    public static int asInt(Long n) {
        if (n == null) {
            return 0;
        }
        return (int) Math.max(Math.min(n, Integer.MAX_VALUE), Integer.MIN_VALUE);
    }

}
