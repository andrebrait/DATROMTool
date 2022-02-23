package io.github.datromtool.display;

public abstract class CachingDisplayable implements Displayable {

    private transient String $displayNameCache;

    @Override
    public final String getDisplayName() {
        if ($displayNameCache != null) {
            return $displayNameCache;
        }
        String displayName = getDisplayNameForCache();
        $displayNameCache = displayName;
        return displayName;
    }

    protected abstract String getDisplayNameForCache();
}
