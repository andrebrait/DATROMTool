package io.github.datromtool.display;

import io.github.datromtool.io.Addressable;

public abstract class CachingDisplayableAddressable extends CachingDisplayable implements Addressable {

    @Override
    protected final String getDisplayNameForCache() {
        return getPath().toString();
    }
}
