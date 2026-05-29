package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.github.datromtool.domain.datafile.logiqx.enumerations.YesNo;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@JsonInclude(NON_DEFAULT)
@JsonRootName("biosset")
public record BiosSet(
        @Nonnull
        @JacksonXmlProperty(isAttribute = true)
        @JsonProperty(required = true)
        String name,

        @Nonnull
        @JacksonXmlProperty(isAttribute = true)
        @JsonProperty(required = true)
        String description,

        @Nonnull
        @JacksonXmlProperty(localName = "default", isAttribute = true)
        @JsonProperty(value = "default", defaultValue = "no")
        YesNo isDefault) {

    public BiosSet {
        if (isDefault == null) isDefault = YesNo.NO;
    }
}
