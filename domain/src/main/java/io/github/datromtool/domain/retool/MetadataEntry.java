package io.github.datromtool.domain.retool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

/**
 * A single value in a Retool metadata file's flat title-name-to-entry map (see {@link
 * RetoolMetadata}). The metadata file spec page itself
 * (<a href="https://unexpectedpanda.github.io/retool/contribute-metadata-files/">
 * contribute-metadata-files</a>) does not publish a formal per-entry schema - it only says
 * metadata files "contain scraped data from No-Intro's and Redump's websites" and are generated,
 * not hand-authored - so this shape is reverse-engineered from a real upstream file (see the
 * fixture Javadoc in core's test suite for provenance): each entry is {@code {"languages": [...
 * two-or-three-letter, title-case codes like "En", "Fr", "De", or the literal "nolang" ...]}}.
 * Codes are modeled as plain strings, not an enum - they are not strict ISO-639-1 (e.g.
 * {@code "nolang"}) and interpreting them is step 2/3's concern.
 */
@JsonInclude(NON_DEFAULT)
public record MetadataEntry(@Nonnull ImmutableList<String> languages) {

    public MetadataEntry {
        if (languages == null) languages = ImmutableList.of();
    }
}
