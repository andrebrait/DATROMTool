package io.github.datromtool;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SerializationHelper {

    private final static Logger logger = LoggerFactory.getLogger(SerializationHelper.class);

    private final XmlMapper xmlMapper;
    private final JsonMapper jsonMapper;

    public static SerializationHelper getInstance() {
        return SerializationHelperHolder.INSTANCE;
    }

    private final static class SerializationHelperHolder {

        private final static SerializationHelper INSTANCE = new SerializationHelper();
    }

    private SerializationHelper() {
        this.xmlMapper = createXmlMapper();
        this.jsonMapper = createJsonMapper();
    }

    private static XmlMapper createXmlMapper() {
        return XmlMapper.builder()
                .defaultUseWrapper(false)
                .addModule(new JaxbAnnotationModule())
                .addModule(new JavaTimeModule())
                .addModule(new Jdk8Module())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    private static JsonMapper createJsonMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(new Jdk8Module())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    private static final class LoggingErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException exception) {
            logger.warn("XML parsing warning", exception);
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            logger.error("XML parsing error", exception);
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            logger.error("XML parsing fatal error", exception);
            throw exception;
        }
    }

    public <T> T loadXml(Path xml, Class<T> tClass) throws Exception {
        try (InputStream inputStream = Files.newInputStream(xml)) {
            return xmlMapper.readValue(inputStream, tClass);
        }
    }

    public <T> T loadJson(Path json, Class<T> tClass) throws Exception {
        try (InputStream inputStream = Files.newInputStream(json)) {
            return jsonMapper.readValue(inputStream, tClass);
        }
    }
}
