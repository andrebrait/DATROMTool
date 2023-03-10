
package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.datromtool.domain.datafile.logiqx.enumerations.ForceMerging;
import io.github.datromtool.domain.datafile.logiqx.enumerations.ForceNoDump;
import io.github.datromtool.domain.datafile.logiqx.enumerations.ForcePacking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
@JacksonXmlRootElement(localName = "clrmamepro")
public class Clrmamepro {

    @JacksonXmlProperty(localName = "header", isAttribute = true)
    @JsonProperty("header")
    String headerFile;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "forcemerging", isAttribute = true)
    @JsonProperty(defaultValue = "split")
    ForceMerging forceMerging = ForceMerging.SPLIT;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "forcenodump", isAttribute = true)
    @JsonProperty(defaultValue = "obsolete")
    ForceNoDump forceNoDump = ForceNoDump.OBSOLETE;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "forcepacking", isAttribute = true)
    @JsonProperty(defaultValue = "zip")
    ForcePacking forcePacking = ForcePacking.ZIP;

}
