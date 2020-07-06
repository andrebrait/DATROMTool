
package io.github.datromtool.domain.datafile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.domain.datafile.enumerations.YesNo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@JsonPropertyOrder({
        "comments",
        "description",
        "year",
        "manufacturer",
        "releases",
        "biosSets",
        "roms",
        "disks",
        "samples",
        "archives"
})
@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JacksonXmlRootElement(localName = "game")
@JsonInclude(NON_DEFAULT)
public class Game {

    @NonNull
    @JacksonXmlProperty(localName = "comment")
    ImmutableList<String> comments = ImmutableList.of();

    @NonNull
    @JsonProperty(required = true)
    String description;
    String year;
    String manufacturer;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "release")
    ImmutableList<Release> releases = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "biosset")
    ImmutableList<BIOSSet> BIOSSets = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "rom")
    ImmutableList<ROM> roms = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "disk")
    ImmutableList<Disk> disks = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "sample")
    ImmutableList<Sample> samples = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "archive")
    ImmutableList<Archive> archives = ImmutableList.of();

    @NonNull
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(required = true)
    String name;

    @JacksonXmlProperty(localName = "sourcefile", isAttribute = true)
    String sourceFile;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "isbios", isAttribute = true)
    @JsonProperty(defaultValue = "no")
    YesNo isBIOS = YesNo.NO;

    @JacksonXmlProperty(localName = "cloneof", isAttribute = true)
    String cloneOf;

    @JacksonXmlProperty(localName = "romof", isAttribute = true)
    String romOf;

    @JacksonXmlProperty(localName = "sampleof", isAttribute = true)
    String sampleOf;

    @JacksonXmlProperty(isAttribute = true)
    String board;

    @JacksonXmlProperty(localName = "rebuildto", isAttribute = true)
    String rebuildTo;

}
