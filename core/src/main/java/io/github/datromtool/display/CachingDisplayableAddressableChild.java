package io.github.datromtool.display;

import io.github.datromtool.io.AddressableChild;

import javax.annotation.Nonnull;

public abstract class CachingDisplayableAddressableChild extends CachingDisplayable implements AddressableChild {

    @Nonnull
    @Override
    protected final String getDisplayNameForCache() {
        return getParent().getPath().resolve(getRelativeName()).toString();
    }
}
