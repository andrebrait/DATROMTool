package io.github.datromtool.util;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;

class XMLValidatorTest {

    @Test
    void validateDat() throws URISyntaxException, IOException, SAXException {
        XMLValidator.validateDat(PathUtils.fromClassPath("valid_standard.dat"));
    }

    @Test
    void validateHeader() {
    }
}