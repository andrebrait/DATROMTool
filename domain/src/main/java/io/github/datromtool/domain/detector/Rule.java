
package io.github.datromtool.domain.detector;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Bytes;
import io.github.datromtool.domain.detector.enumerations.BinaryOperation;
import io.github.datromtool.domain.detector.exception.RuleException;
import io.github.datromtool.domain.detector.util.NumberUtils;
import io.github.datromtool.domain.serialization.HexDeserializer;
import io.github.datromtool.domain.serialization.HexSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@JsonPropertyOrder({
        "dataTests",
        "orTests",
        "xorTests",
        "andTests",
        "fileTests"
})
@Value
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
@JacksonXmlRootElement(localName = "rule")
public class Rule {

    public static final String END_OF_FILE = "EOF";

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "data")
    ImmutableList<DataTest> dataTests = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "or")
    ImmutableList<OrTest> orTests = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "xor")
    ImmutableList<XorTest> xorTests = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "and")
    ImmutableList<AndTest> andTests = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "file")
    ImmutableList<FileTest> fileTests = ImmutableList.of();

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "start_offset", isAttribute = true)
    @JsonProperty(defaultValue = "0")
    @JsonSerialize(using = HexSerializer.class)
    @JsonDeserialize(using = HexDeserializer.class)
    Long startOffset = 0L;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(localName = "end_offset", isAttribute = true)
    @JsonProperty(defaultValue = END_OF_FILE)
    @JsonSerialize(using = HexSerializer.class)
    @JsonDeserialize(using = HexDeserializer.class)
    Long endOffset = Long.MAX_VALUE;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(defaultValue = "none")
    BinaryOperation operation = BinaryOperation.NONE;

    private static boolean testAll(List<? extends Test> tests, byte[] bytes, int actualLength) {
        return tests.stream().allMatch(t -> t.test(bytes, actualLength));
    }

    public byte[] apply(byte[] bytes, int actualLength) {
        actualLength = Math.min(bytes.length, actualLength);
        if (testAll(dataTests, bytes, actualLength)
                && testAll(fileTests, bytes, actualLength)
                && testAll(andTests, bytes, actualLength)
                && testAll(orTests, bytes, actualLength)
                && testAll(xorTests, bytes, actualLength)) {
            int startOffset = NumberUtils.asInt(this.startOffset);
            if (startOffset < 0) {
                startOffset += actualLength;
            }
            startOffset = Math.max(startOffset, 0);
            int endOffset = NumberUtils.asInt(this.endOffset);
            if (endOffset < 0) {
                endOffset += actualLength;
            }
            endOffset = Math.min(endOffset, actualLength);
            if (startOffset != 0
                    || endOffset != actualLength
                    || operation != BinaryOperation.NONE) {
                byte[] out = Arrays.copyOfRange(bytes, startOffset, endOffset);
                switch (operation) {
                    case BIT_SWAP:
                        for (int i = 0; i < out.length; i++) {
                            out[i] = (byte) (Integer.reverse(out[i] << 24) & 0xFF);
                        }
                        break;
                    case BYTE_SWAP:
                        if (out.length % 2 != 0) {
                            throw new RuleException(this, "Array size is not a multiple of 2");
                        }
                        swap(out, 2);
                        break;
                    case WORD_SWAP:
                        if (out.length % 4 != 0) {
                            throw new RuleException(this, "Array size is not a multiple of 4");
                        }
                        swap(out, 4);
                        break;
                    case WORD_BYTE_SWAP:
                        if (out.length % 4 != 0) {
                            throw new RuleException(this, "Array size is not a multiple of 4");
                        }
                        swap(out, 4);
                        swap(out, 2);
                        break;
                }
                return out;
            }
        }
        return bytes;
    }

    private static void swap(byte[] bytes, int chunkSize) {
        for (int i = 0; i < bytes.length - chunkSize; i += chunkSize) {
            Bytes.reverse(bytes, i, i + chunkSize);
        }
    }

}
