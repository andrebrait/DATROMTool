package io.github.datromtool.data;

import io.github.datromtool.util.UntrustedPatterns;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * A structured, serialization-friendly stand-in for a raw compiled {@link Pattern}: a
 * {@link #getValue()} plus a {@link MatchType} tag, so profile files (issue #15) can express
 * {@code {value, type}} instead of a {@code \Q...\E}-quoted regex string.
 *
 * <p>The {@link Pattern} is compiled once at construction (never lazily re-derived on every
 * match) and is excluded from {@link #equals(Object)}/{@link #hashCode()}/{@link #toString()}
 * and from JSON/YAML serialization, so two matchers with the same value and type are
 * interchangeable regardless of the underlying compiled instance.
 */
@Getter
@EqualsAndHashCode
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
public final class NameMatcher {

    public enum MatchType {
        LITERAL,
        REGEX;

        @JsonValue
        public String toJson() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    @NonNull
    private final String value;

    @NonNull
    private final MatchType type;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private final Pattern pattern;

    @JsonCreator
    public static NameMatcher of(
            @JsonProperty("value") String value,
            @JsonProperty("type") MatchType type) {
        if (value == null) {
            throw new IllegalArgumentException("NameMatcher value must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("NameMatcher type must not be null");
        }
        return new NameMatcher(value, type, compile(value, type));
    }

    public static NameMatcher literal(String value) {
        return of(value, MatchType.LITERAL);
    }

    public static NameMatcher regex(String value) {
        return of(value, MatchType.REGEX);
    }

    private static Pattern compile(String value, MatchType type) {
        try {
            return switch (type) {
                case LITERAL -> Pattern.compile(Pattern.quote(value));
                case REGEX -> UntrustedPatterns.compile(value);
            };
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException(
                    "Invalid regular expression for NameMatcher value '" + value + "': "
                            + e.getMessage(),
                    e);
        }
    }
}
