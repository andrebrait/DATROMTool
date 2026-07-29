package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.domain.datafile.logiqx.enumerations.Status;
import io.github.datromtool.domain.datafile.logiqx.enumerations.YesNo;
import io.github.datromtool.domain.serialization.SpacedHexArrayDeserializer;
import io.github.datromtool.domain.serialization.SpacedHexArraySerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@JsonInclude(NON_DEFAULT)
@JsonRootName("rom")
public record Rom(
        @Nonnull
        @JacksonXmlProperty(isAttribute = true)
        @JsonProperty(required = true)
        String name,

        /**
         * Optional for parsing: No-Intro uses nodumps without setting a size.
         * Should never be empty for real ROMs or output.
         */
        @JacksonXmlProperty(isAttribute = true)
        Long size,

        /**
         * An immutable list rather than a {@code byte[]}: a record's generated equality
         * compares an array component by identity, so two ROMs with the same header would
         * not be equal. Headers are a handful of bytes, so boxing is irrelevant here.
         */
        @JacksonXmlProperty(isAttribute = true)
        @JsonSerialize(using = SpacedHexArraySerializer.class)
        @JsonDeserialize(using = SpacedHexArrayDeserializer.class)
        ImmutableList<Byte> header,

        @Nonnull
        @JacksonXmlProperty(isAttribute = true)
        @JsonProperty(defaultValue = "no")
        YesNo mia,

        @JacksonXmlProperty(isAttribute = true)
        String crc,

        @JacksonXmlProperty(isAttribute = true)
        String md5,

        @JacksonXmlProperty(isAttribute = true)
        String sha1,

        @JacksonXmlProperty(isAttribute = true)
        String sha256,

        @JacksonXmlProperty(isAttribute = true)
        String merge,

        @JacksonXmlProperty(isAttribute = true)
        Status status,

        @JacksonXmlProperty(isAttribute = true)
        String date,

        @JacksonXmlProperty(isAttribute = true)
        String serial) {

    public Rom {
        if (mia == null) mia = YesNo.NO;
    }

    public Rom(String name, Long size) {
        this(name, size, null, YesNo.NO, null, null, null, null, null, null, null, null);
    }
}
