package io.github.datromtool.cli.argument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.domain.datafile.Datafile;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.io.IOException;
import java.nio.file.Path;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
public final class DatafileArgument {

    private final Path path;

    @ToString.Exclude
    @JsonIgnore
    private final Datafile datafile;

    @JsonCreator
    public static DatafileArgument from(@JsonProperty("path") Path path) throws IOException {
        Datafile datafile = SerializationHelper.getInstance().loadXml(path, Datafile.class);
        return new DatafileArgument(path, datafile);
    }

}

