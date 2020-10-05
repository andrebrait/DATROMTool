package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.datromtool.io.OutputMode;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Jacksonized
@Value
@Builder(toBuilder = true)
@JsonInclude(NON_NULL)
public class TextOutput {

    Path outputFile;
    OutputMode outputMode;
}
