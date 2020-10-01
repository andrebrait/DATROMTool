package io.github.datromtool.cli;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.Maps;
import io.github.datromtool.SerializationHelper;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;

public final class GitVersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        SerializationHelper serializationHelper = SerializationHelper.getInstance();
        List<String> lines = new ArrayList<>();
        lines.add(String.format("DATROMTool v%s", serializationHelper.getVersionString()));
        lines.add("Build information:");
        lines.add(new JsonMapper()
                .enable(ORDER_MAP_ENTRIES_BY_KEYS, INDENT_OUTPUT)
                .writeValueAsString(Maps.filterKeys(
                        serializationHelper.getGitProps(),
                        k -> !SerializationHelper.GIT_BUILD_VERSION.equals(k))));
        return lines.toArray(new String[0]);
    }
}
