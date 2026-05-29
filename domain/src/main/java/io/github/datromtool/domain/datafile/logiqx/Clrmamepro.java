package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.github.datromtool.domain.datafile.logiqx.enumerations.ForceMerging;
import io.github.datromtool.domain.datafile.logiqx.enumerations.ForceNoDump;
import io.github.datromtool.domain.datafile.logiqx.enumerations.ForcePacking;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@JsonInclude(NON_DEFAULT)
@JsonRootName("clrmamepro")
public record Clrmamepro(
        @JacksonXmlProperty(localName = "header", isAttribute = true)
        @JsonProperty("header")
        String headerFile,

        @Nonnull
        @JacksonXmlProperty(localName = "forcemerging", isAttribute = true)
        @JsonProperty(defaultValue = "split")
        ForceMerging forceMerging,

        @Nonnull
        @JacksonXmlProperty(localName = "forcenodump", isAttribute = true)
        @JsonProperty(defaultValue = "obsolete")
        ForceNoDump forceNoDump,

        @Nonnull
        @JacksonXmlProperty(localName = "forcepacking", isAttribute = true)
        @JsonProperty(defaultValue = "zip")
        ForcePacking forcePacking) {

    public Clrmamepro {
        if (forceMerging == null) forceMerging = ForceMerging.SPLIT;
        if (forceNoDump == null) forceNoDump = ForceNoDump.OBSOLETE;
        if (forcePacking == null) forcePacking = ForcePacking.ZIP;
    }
}
