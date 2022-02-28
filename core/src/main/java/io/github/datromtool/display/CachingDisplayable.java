package io.github.datromtool.display;

public abstract class CachingDisplayable implements Displayable {

    private transient String $displayNameCache;

    @Override
    public final String getDisplayName() {
        if ($displayNameCache == null) {
            $displayNameCache = getDisplayNameForCache();
        }
        return $displayNameCache;
    }

    protected abstract String getDisplayNameForCache();
}
