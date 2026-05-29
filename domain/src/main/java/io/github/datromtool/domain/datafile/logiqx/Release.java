package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.github.datromtool.domain.datafile.logiqx.enumerations.YesNo;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@JsonInclude(NON_DEFAULT)
@JsonRootName("release")
public record Release(
        @Nonnull
        @JacksonXmlProperty(isAttribute = true)
        @JsonProperty(required = true)
        String name,

        @Nonnull
        @JacksonXmlProperty(isAttribute = true)
        @JsonProperty(required = true)
        String region,

        @JacksonXmlProperty(isAttribute = true)
        String language,

        @JacksonXmlProperty(isAttribute = true)
        String date,

        @Nonnull
        @JacksonXmlProperty(localName = "default", isAttribute = true)
        @JsonProperty(defaultValue = "no")
        YesNo isDefault) {

    public Release {
        if (isDefault == null) isDefault = YesNo.NO;
    }
}
