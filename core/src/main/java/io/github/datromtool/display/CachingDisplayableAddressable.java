package io.github.datromtool.display;

import io.github.datromtool.io.Addressable;

import javax.annotation.Nonnull;

public abstract class CachingDisplayableAddressable extends CachingDisplayable implements Addressable {

    @Nonnull
    @Override
    protected final String getDisplayNameForCache() {
        return getPath().toString();
    }
}
