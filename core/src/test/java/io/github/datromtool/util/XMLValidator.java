package io.github.datromtool.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class XMLValidator {

    private XMLValidator() {
    }

    private final static Logger logger = LoggerFactory.getLogger(XMLValidator.class);

    public static void validateDat(byte[] xml) throws SAXException, IOException {
        validateDat(xml, "xsd/datafile/datafile.xsd");
    }

    public static void validateDetector(byte[] xml) throws SAXException, IOException {
        validateDat(xml, "xsd/detector/detector.xsd");
    }

    private static void validateDat(byte[] xml, String xsd) throws SAXException, IOException {
        try {
            validate(xml, Paths.get(ClassLoader.getSystemResource(xsd).toURI()));
        } catch (URISyntaxException e) {
            logger.error("Could not open XSD file", e);
        }
    }

    private static void validate(byte[] xml, Path xsd) throws SAXException, IOException {
        // create a SchemaFactory capable of understanding WXS schemas
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // load a WXS schema, represented by a Schema instance
        try (BufferedReader xsdReader = Files.newBufferedReader(xsd)) {
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
