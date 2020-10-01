package io.github.datromtool.util;

import javax.annotation.Nonnull;

public final class ArgumentException extends Exception {

    public ArgumentException(@Nonnull String message, @Nonnull Throwable cause) {
        super(message, cause);
    }

    @Override
    @Nonnull
    public Throwable getCause() {
        return super.getCause();
    }
}
