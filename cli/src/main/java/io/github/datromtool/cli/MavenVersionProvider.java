package io.github.datromtool.cli;

import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class MavenVersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        Path path = Paths.get(ClassLoader.getSystemResource("filtered/VERSION").toURI());
        return Files.readAllLines(path).toArray(new String[0]);
    }
}
