package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.github.datromtool.domain.datafile.logiqx.enumerations.Status;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@JsonInclude(NON_DEFAULT)
@JsonRootName("disk")
public record Disk(
        @Nonnull
        @JacksonXmlProperty(isAttribute = true)
        @JsonProperty(required = true)
        String name,

        @JacksonXmlProperty(isAttribute = true)
        String sha1,

        @JacksonXmlProperty(isAttribute = true)
        String sha256,

        @JacksonXmlProperty(isAttribute = true)
        String md5,

        @JacksonXmlProperty(isAttribute = true)
        String merge,

        @Nonnull
        @JacksonXmlProperty(isAttribute = true)
        @JsonProperty(defaultValue = "good")
        Status status) {

    public Disk {
        if (status == null) status = Status.GOOD;
    }
}
