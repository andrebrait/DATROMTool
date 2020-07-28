
package io.github.datromtool.domain.detector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.datromtool.domain.detector.enumerations.ComparisonOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@Value
@Jacksonized
@SuperBuilder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
@EqualsAndHashCode(callSuper = true)
@JacksonXmlRootElement(localName = "data")
public class FileTest extends Test {

    private static final String POWER_OF_TWO = "PO2";

    @NonNull
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(required = true)
    String size;

    @NonNull
    @Builder.Default
    @JacksonXmlProperty(isAttribute = true)
    @JsonProperty(defaultValue = "equal")
    ComparisonOperator operator = ComparisonOperator.EQUAL;

    @JsonIgnore
    public long getSizeAsLong() {
        return size != null ? Long.parseLong(size, 16) : 0;
    }

    @Override
    public boolean test(byte[] bytes, int actualLength) {
        if (POWER_OF_TWO.equals(size)) {
            double log = Math.log(actualLength) / Math.log(2);
            return Math.abs(Math.round(log) - log) < 1e-11;
        }
        switch (operator) {
            case LESS:
                return (actualLength < getSizeAsLong()) == getResult();
            case GREATER:
                return (actualLength > getSizeAsLong()) == getResult();
            case EQUAL:
                return (actualLength == getSizeAsLong()) == getResult();
            default:
                return false;
        }
    }
}
