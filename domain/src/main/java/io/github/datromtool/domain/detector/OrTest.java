
package io.github.datromtool.domain.detector;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
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
@JacksonXmlRootElement(localName = "or")
public class OrTest extends LogicalTest {

    @Override
    protected byte operate(byte a, byte b) {
        return (byte) (a | b);
    }

}
