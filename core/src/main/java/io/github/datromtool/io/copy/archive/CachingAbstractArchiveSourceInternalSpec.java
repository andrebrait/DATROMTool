package io.github.datromtool.io.copy.archive;

import javax.annotation.Nonnull;

public abstract class CachingAbstractArchiveSourceInternalSpec extends AbstractArchiveSourceInternalSpec {

    private transient String $nameCache;

    @Override
    public final String getName() {
        if ($nameCache == null) {
            $nameCache = getNameForCache();
        }
        return $nameCache;
    }

    @Nonnull
    protected abstract String getNameForCache();
}
