package io.github.datromtool.cli;

import com.fasterxml.jackson.databind.json.JsonMapper;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;

final class GitVersionProvider implements CommandLine.IVersionProvider {

    public static final String TITLE = "DATROMTool - *that* tool to work with DATs and ROMs!";

    @Override
    public String[] getVersion() throws Exception {
        Properties gitProps = new Properties();
        gitProps.load(ClassLoader.getSystemResourceAsStream("git.properties"));
        List<String> lines = new ArrayList<>();
        lines.add(TITLE);
        lines.add("Build information:");
        lines.add(new JsonMapper()
                .enable(ORDER_MAP_ENTRIES_BY_KEYS, INDENT_OUTPUT)
                .writeValueAsString(gitProps));
        return lines.toArray(new String[0]);
    }
}
