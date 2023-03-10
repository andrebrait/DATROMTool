package io.github.datromtool.util;

import lombok.NoArgsConstructor;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class XMLValidator {

    public static void validateDat(byte[] xml) throws Exception {
        validateDat(xml, "xsd/datafile/logiqx/datafile.xsd");
    }

    public static void validateDetector(byte[] xml) throws Exception {
        validateDat(xml, "xsd/detector/detector.xsd");
    }

    private static void validateDat(byte[] xml, String xsd) throws Exception {
        validate(xml, ClassLoader.getSystemResource(xsd));
    }

    private static void validate(byte[] xml, URL url) throws Exception {
        // create a SchemaFactory capable of understanding WXS schemas
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // load a WXS schema, represented by a Schema instance
        try (BufferedReader xsdReader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            Source schemaFile = new StreamSource(xsdReader);
            Schema schema = factory.newSchema(schemaFile);

            // create a Validator instance, which can be used to validate an instance document
            Validator validator = schema.newValidator();

            // validate the DOM tree
            try (InputStream stream = new ByteArrayInputStream(xml)) {
                validator.validate(new StreamSource(stream));
            }
        }
    }

}
