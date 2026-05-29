package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.github.datromtool.domain.datafile.logiqx.enumerations.SampleMode;
import io.github.datromtool.domain.datafile.logiqx.enumerations.SetMode;
import io.github.datromtool.domain.datafile.logiqx.enumerations.YesNo;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@JsonInclude(NON_DEFAULT)
@JsonRootName("romcenter")
public record RomCenter(
        @JacksonXmlProperty(isAttribute = true)
        String plugin,

        @Nonnull
        @JacksonXmlProperty(localName = "rommode", isAttribute = true)
        @JsonProperty(defaultValue = "split")
        SetMode romMode,

        @Nonnull
        @JacksonXmlProperty(localName = "biosmode", isAttribute = true)
        @JsonProperty(defaultValue = "split")
        SetMode biosMode,

        @Nonnull
        @JacksonXmlProperty(localName = "samplemode", isAttribute = true)
        @JsonProperty(defaultValue = "merged")
        SampleMode sampleMode,

        @Nonnull
        @JacksonXmlProperty(localName = "lockrommode", isAttribute = true)
        @JsonProperty(defaultValue = "no")
        YesNo isLockRomMode,

        @Nonnull
        @JacksonXmlProperty(localName = "lockbiosmode", isAttribute = true)
        @JsonProperty(defaultValue = "no")
        YesNo isLockBiosMode,

        @Nonnull
        @JacksonXmlProperty(localName = "locksamplemode", isAttribute = true)
        @JsonProperty(defaultValue = "no")
        YesNo isLockSampleMode) {

    public RomCenter {
        if (romMode == null) romMode = SetMode.SPLIT;
        if (biosMode == null) biosMode = SetMode.SPLIT;
        if (sampleMode == null) sampleMode = SampleMode.MERGED;
        if (isLockRomMode == null) isLockRomMode = YesNo.NO;
        if (isLockBiosMode == null) isLockBiosMode = YesNo.NO;
        if (isLockSampleMode == null) isLockSampleMode = YesNo.NO;
    }
}
