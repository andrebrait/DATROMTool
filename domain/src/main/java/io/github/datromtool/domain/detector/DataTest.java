
package io.github.datromtool.domain.detector;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.datromtool.domain.detector.util.NumberUtils;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static lombok.AccessLevel.PRIVATE;

@Value
@Jacksonized
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonInclude(NON_DEFAULT)
@EqualsAndHashCode(callSuper = true)
@JacksonXmlRootElement(localName = "data")
public class DataTest extends BinaryTest {

    @Override
    public boolean test(byte[] bytes, int actualLength) {
        actualLength = Math.min(bytes.length, actualLength);
        int offset = NumberUtils.asInt(getOffset());
        if (offset < 0) {
            offset += actualLength;
        }
        if (offset < 0 || actualLength - offset < getValue().length) {
            return false;
        }
        boolean matches = true;
        for (int i = 0; matches && i < getValue().length; i++) {
            matches = getValue()[i] == bytes[i + offset];
        }
        return matches == getResult();
    }
}
