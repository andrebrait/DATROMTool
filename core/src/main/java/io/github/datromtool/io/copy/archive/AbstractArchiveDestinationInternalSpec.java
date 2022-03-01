package io.github.datromtool.io.copy.archive;

import io.github.datromtool.display.CachingDisplayableAddressableChild;

public abstract class AbstractArchiveDestinationInternalSpec extends CachingDisplayableAddressableChild implements ArchiveDestinationInternalSpec {

    @Override
    public final String getRelativeName() {
        return getName();
    }
}
