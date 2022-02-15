package io.github.datromtool.cli.argument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.datromtool.SerializationHelper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
public final class PatternsFileArgument {

    private final Path path;
    @JsonIgnore
    private final StringFilterArgument stringFilter;

    @JsonCreator
    public static PatternsFileArgument from(@JsonProperty("path") Path path) throws IOException {
        StringFilterArgument stringFilter = SerializationHelper.getInstance().loadJsonOrYaml(path, StringFilterArgument.class);
        return new PatternsFileArgument(path, stringFilter);
    }

}

