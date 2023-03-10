
package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.datromtool.domain.datafile.logiqx.enumerations.SampleMode;
import io.github.datromtool.domain.datafile.logiqx.enumerations.SetMode;
import io.github.datromtool.domain.datafile.logiqx.enumerations.YesNo;
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
@JacksonXmlRootElement(localName = "romcenter")
public class RomCenter {

    @JacksonXmlProperty(isAttribute = true)
    String plugin;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "rommode", isAttribute = true)
    @JsonProperty(defaultValue = "split")
    SetMode romMode = SetMode.SPLIT;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "biosmode", isAttribute = true)
    @JsonProperty(defaultValue = "split")
    SetMode biosMode = SetMode.SPLIT;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "samplemode", isAttribute = true)
    @JsonProperty(defaultValue = "merged")
    SampleMode sampleMode = SampleMode.MERGED;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "lockrommode", isAttribute = true)
    @JsonProperty(defaultValue = "no")
    YesNo isLockRomMode = YesNo.NO;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "lockbiosmode", isAttribute = true)
    @JsonProperty(defaultValue = "no")
    YesNo isLockBiosMode = YesNo.NO;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "locksamplemode", isAttribute = true)
    @JsonProperty(defaultValue = "no")
    YesNo isLockSampleMode = YesNo.NO;

}
