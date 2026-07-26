package io.github.datromtool.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.data.Filter;
import io.github.datromtool.data.FileOutputOptions;
import io.github.datromtool.data.PostFilter;
import io.github.datromtool.data.SortingPreference;
import io.github.datromtool.data.TextOutputOptions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

/**
 * The settings contract shared by the CLI and the future GUI (issue #15): a serializable
 * aggregate composing every pipeline stage's existing configuration object, so a profile file
 * (JSON or YAML, dispatched by {@link io.github.datromtool.SerializationHelper#loadJsonOrYaml})
 * is the direct serialized form of a run's configuration — no parallel schema. Every section is
 * independently optional and falls back to that stage's own defaults when omitted, exactly like
 * omitting the equivalent CLI flag group today.
 *
 * <p>When multiple profile files are layered (e.g. {@code --profile} given more than once, later
 * files taking precedence), they are combined with a documented, format-agnostic deep-merge
 * performed on the raw JSON/YAML tree before binding to this type (see
 * {@link io.github.datromtool.SerializationHelper#loadProfiles}): objects merge recursively,
 * field by field, so setting one field of a section in a later file leaves that section's other
 * fields — set by an earlier file — untouched; everything else (scalars, and lists/sets such as
 * {@code sort.regions} or {@code filter.includes}) is replaced wholesale by the later file's
 * value, never appended to or spliced with the earlier one. A section entirely absent from a
 * later file leaves the earlier file's section untouched.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
public class Profile {

    @Value
    @Jacksonized
    @Builder(toBuilder = true)
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PRIVATE, force = true)
    @JsonInclude(NON_DEFAULT)
    public static class InputSection {

        @NonNull
        @Builder.Default
        ImmutableList<Path> dats = ImmutableList.of();

        @NonNull
        @Builder.Default
        ImmutableList<Path> dirs = ImmutableList.of();
    }

    /**
     * {@link #getFile()} and {@link #getText()} are mutually exclusive — a run either produces
     * archived/copied file output or a text report, never both. Neither is required: an
     * {@code output} section with both unset is valid and means "no output configured yet"
     * (e.g. a partial GUI-authored profile). Call {@link #validate()} (or
     * {@link Profile#validate()}) after loading to enforce the exclusivity; construction itself
     * does not throw, since Jackson must be able to bind an (invalid) profile before the caller
     * gets a chance to report a file-path-scoped error.
     */
    @Value
    @Jacksonized
    @Builder(toBuilder = true)
    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PRIVATE, force = true)
    @JsonInclude(NON_DEFAULT)
    public static class OutputSection {

        FileOutputOptions file;

        TextOutputOptions text;

        /**
         * @throws IllegalArgumentException if both {@link #getFile()} and {@link #getText()}
         *                                   are set.
         */
        public void validate() {
            if (file != null && text != null) {
                throw new IllegalArgumentException(
                        "output.file and output.text are mutually exclusive");
            }
        }
    }

    @NonNull
    @Builder.Default
    InputSection input = InputSection.builder().build();

    @NonNull
    @Builder.Default
    Filter filter = Filter.builder().build();

    @NonNull
    @Builder.Default
    SortingPreference sort = SortingPreference.builder().build();

    @NonNull
    @Builder.Default
    PostFilter postFilter = PostFilter.builder().build();

    @NonNull
    @Builder.Default
    OutputSection output = OutputSection.builder().build();

    @NonNull
    @Builder.Default
    AppConfig performance = AppConfig.builder().build();

    /**
     * @throws IllegalArgumentException if {@link #getOutput()} has both {@code file} and
     *                                   {@code text} set.
     */
    public void validate() {
        output.validate();
    }
}
