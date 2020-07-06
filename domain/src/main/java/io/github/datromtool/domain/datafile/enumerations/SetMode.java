package io.github.datromtool.domain.datafile.enumerations;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SetMode {
    @JsonProperty("merged")
    MERGED,
    @JsonProperty("split")
    SPLIT,
    @JsonProperty("unmerged")
    UNMERGED

}
