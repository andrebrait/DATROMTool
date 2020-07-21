package io.github.datromtool;

import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

import static lombok.AccessLevel.PRIVATE;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;


@NoArgsConstructor(access = PRIVATE)
public final class Patterns {

    @SuppressWarnings("RegExpUnexpectedAnchor")
    public final static Pattern NO_MATCH =
            compile("a^");

    /*
     * Parsing regions and languages
     */
    public final static Pattern SECTIONS =
            compile("\\(([^()]+)\\)");
    public final static Pattern LANGUAGES =
            compile("\\(([a-z]{2}(?:[,+][a-z]{2})*)\\)", CASE_INSENSITIVE);

    /*
     * Filters
     */
    public final static Pattern BIOS =
            compile(quote("[BIOS]"), CASE_INSENSITIVE);
    public final static Pattern PROGRAM =
            compile("\\((?:Test\\s*)?Program\\)", CASE_INSENSITIVE);
    public final static Pattern ENHANCEMENT_CHIP =
            compile("\\(Enhancement\\s*Chip\\)", CASE_INSENSITIVE);
    public final static Pattern UNLICENSED =
            compile(quote("(Unl)"), CASE_INSENSITIVE);
    public final static Pattern PIRATE =
            compile(quote("(Pirate)"), CASE_INSENSITIVE);
    public final static Pattern PROMO =
            compile(quote("(Promo)"), CASE_INSENSITIVE);
    public final static Pattern DLC =
            compile(quote("(DLC)"), CASE_INSENSITIVE);
    public final static Pattern UPDATE =
            compile(quote("(Update)"), CASE_INSENSITIVE);
    public final static Pattern BAD =
            compile(quote("[b]"), CASE_INSENSITIVE);

    /*
     * Parsing versions
     */
    public final static Pattern PROTO =
            compile("\\(Proto(?:\\s*([a-z0-9.]+))?\\)", CASE_INSENSITIVE);
    public final static Pattern BETA =
            compile("\\(Beta(?:\\s*([a-z0-9.]+))?\\)", CASE_INSENSITIVE);
    public final static Pattern DEMO =
            compile("\\(Demo(?:\\s*([a-z0-9.]+))?\\)", CASE_INSENSITIVE);
    public final static Pattern SAMPLE =
            compile("\\(Sample(?:\\s*([a-z0-9.]+))?\\)", CASE_INSENSITIVE);
    public final static Pattern REVISION =
            compile("\\(Rev\\s*([a-z0-9.]+)\\)", CASE_INSENSITIVE);
    public final static Pattern VERSION =
            compile("\\(v\\s*([a-z0-9.]+)(?:,\\s*v\\s*([a-z0-9.]+))?\\)", CASE_INSENSITIVE);

    /*
     * Archive formats
     */
    public final static Pattern ZIP =
            compile("\\.zip$", CASE_INSENSITIVE);
    public final static Pattern RAR =
            compile("\\.rar$", CASE_INSENSITIVE);
    public final static Pattern SEVEN_ZIP =
            compile("\\.7z$", CASE_INSENSITIVE);
    public final static Pattern TAR_ARCHIVE =
            compile("\\.tar(?:\\.[^.]+)?$", CASE_INSENSITIVE);

    public final static Pattern TAR_GZ =
            compile("\\.tar\\.gz$", CASE_INSENSITIVE);
    public final static Pattern TAR_XZ =
            compile("\\.tar\\.xz$", CASE_INSENSITIVE);
    public final static Pattern TAR_BZ2 =
            compile("\\.tar\\.bz2$", CASE_INSENSITIVE);
    public final static Pattern TAR_LZMA =
            compile("\\.tar\\.lzma$", CASE_INSENSITIVE);
    public final static Pattern TAR_LZ4 =
            compile("\\.tar\\.lz4$", CASE_INSENSITIVE);
    public final static Pattern TAR_UNCOMPRESSED =
            compile("\\.tar$", CASE_INSENSITIVE);
}
