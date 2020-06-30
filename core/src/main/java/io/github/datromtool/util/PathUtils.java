package io.github.datromtool.util;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class PathUtils {

    private PathUtils() {
    }

    public static Path fromClassPath(String path) throws URISyntaxException {
        return Paths.get(ClassLoader.getSystemResource(path).toURI());
    }
}
