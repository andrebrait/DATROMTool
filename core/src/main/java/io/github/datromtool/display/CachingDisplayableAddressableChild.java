package io.github.datromtool.display;

import io.github.datromtool.io.AddressableChild;

public abstract class CachingDisplayableAddressableChild extends CachingDisplayable implements AddressableChild {

    @Override
    protected final String getDisplayNameForCache() {
        return getParent().getPath().resolve(getRelativeName()).toString();
    }
}
