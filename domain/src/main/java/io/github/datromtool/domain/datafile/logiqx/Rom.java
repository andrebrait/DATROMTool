package io.github.datromtool.domain.datafile.logiqx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.github.datromtool.domain.datafile.logiqx.enumerations.Status;
import io.github.datromtool.domain.datafile.logiqx.enumerations.YesNo;
import io.github.datromtool.domain.serialization.SpacedHexArrayDeserializer;
import io.github.datromtool.domain.serialization.SpacedHexArraySerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import javax.annotation.Nonnull;

import java.util.Arrays;
import java.util.Objects;

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

        @JacksonXmlProperty(isAttribute = true)
        @JsonSerialize(using = SpacedHexArraySerializer.class)
        @JsonDeserialize(using = SpacedHexArrayDeserializer.class)
        byte[] header,

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

    // The generated record methods compare header by array identity, which makes two ROMs
    // describing the same dumped file unequal. Header content is part of a ROM's value.

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof Rom other
                && Objects.equals(name, other.name)
                && Objects.equals(size, other.size)
                && Arrays.equals(header, other.header)
                && mia == other.mia
                && Objects.equals(crc, other.crc)
                && Objects.equals(md5, other.md5)
                && Objects.equals(sha1, other.sha1)
                && Objects.equals(sha256, other.sha256)
                && Objects.equals(merge, other.merge)
                && status == other.status
                && Objects.equals(date, other.date)
                && Objects.equals(serial, other.serial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                size,
                Arrays.hashCode(header),
                mia,
                crc,
                md5,
                sha1,
                sha256,
                merge,
                status,
                date,
                serial);
    }

    @Override
    public String toString() {
        return ("Rom[name=%s, size=%s, header=%s, mia=%s, crc=%s, md5=%s, sha1=%s, sha256=%s, "
                + "merge=%s, status=%s, date=%s, serial=%s]")
                .formatted(
                        name,
                        size,
                        Arrays.toString(header),
                        mia,
                        crc,
                        md5,
                        sha1,
                        sha256,
                        merge,
                        status,
                        date,
                        serial);
    }
}
