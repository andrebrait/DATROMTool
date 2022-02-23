package io.github.datromtool.io.spec;

import io.github.datromtool.display.CachingDisplayableAddressableChild;

public abstract class AbstractArchiveDestinationInternalSpec extends CachingDisplayableAddressableChild implements ArchiveDestinationInternalSpec {

    @Override
    public final String getRelativeName() {
        return getName();
    }
}
