package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Jacksonized
@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
public class TextOutputOptions {

    Path outputFile;
    OutputMode outputMode;
}
