package io.github.datromtool.display;

import javax.annotation.Nonnull;

public abstract class CachingDisplayable implements Displayable {

    private transient String $displayNameCache;

    @Override
    public final String getDisplayName() {
        if ($displayNameCache == null) {
            $displayNameCache = getDisplayNameForCache();
        }
        return $displayNameCache;
    }

    @Nonnull
    protected abstract String getDisplayNameForCache();
}
