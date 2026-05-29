package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.nio.file.Path;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record TextOutputOptions(Path outputFile, OutputMode outputMode) {
}
