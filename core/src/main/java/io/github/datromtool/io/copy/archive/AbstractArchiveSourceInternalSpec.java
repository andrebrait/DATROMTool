package io.github.datromtool.io.copy.archive;

import io.github.datromtool.display.CachingDisplayableAddressableChild;

public abstract class AbstractArchiveSourceInternalSpec extends CachingDisplayableAddressableChild implements ArchiveSourceInternalSpec {

    @Override
    public final String getRelativeName() {
        return getName();
    }
}
