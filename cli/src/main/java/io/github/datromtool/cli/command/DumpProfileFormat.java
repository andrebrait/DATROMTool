package io.github.datromtool.cli.command;

/**
 * The two serialization formats {@code --dump-profile} can emit. XML is intentionally not a
 * member: {@link io.github.datromtool.config.Profile} is a settings contract, not a DAT-file
 * representation, so only the two formats {@code SerializationHelper#loadJsonOrYaml} dispatches
 * on are offered here.
 */
public enum DumpProfileFormat {
    JSON,
    YAML
}
