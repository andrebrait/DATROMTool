package io.github.datromtool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import io.github.datromtool.data.RegionData;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
public final class SerializationHelper {

    private final static Logger logger = LoggerFactory.getLogger(SerializationHelper.class);

    private final static Path REGION_DATA_CONFIG_PATH = Paths.get("config", "region-data.yaml");

    private final XmlMapper xmlMapper;
    private final JsonMapper jsonMapper;
    private final YAMLMapper yamlMapper;

    public static SerializationHelper getInstance() {
        return SerializationHelperHolder.INSTANCE;
    }

    private final static class SerializationHelperHolder {

        private final static SerializationHelper INSTANCE = new SerializationHelper();
    }

    private SerializationHelper() {
        this.xmlMapper = createXmlMapper();
        this.jsonMapper = createJsonMapper();
        this.yamlMapper = createYamlMapper();
    }

    private static XmlMapper createXmlMapper() {
        return XmlMapper.builder()
                .defaultUseWrapper(false)
                .addModule(new JaxbAnnotationModule())
                .addModule(new JavaTimeModule())
                .addModule(new Jdk8Module())
                .addModule(new GuavaModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
    }

    private static JsonMapper createJsonMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(new Jdk8Module())
                .addModule(new GuavaModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
    }

    private static YAMLMapper createYamlMapper() {
        return YAMLMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(new Jdk8Module())
                .addModule(new GuavaModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
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

    public <T> T loadYaml(Path yaml, Class<T> tClass) throws Exception {
        try (InputStream inputStream = Files.newInputStream(yaml)) {
            return yamlMapper.readValue(inputStream, tClass);
        }
    }

    public RegionData loadRegionData() throws Exception {
        if (REGION_DATA_CONFIG_PATH.toFile().isFile()) {
            try {
                return loadYaml(REGION_DATA_CONFIG_PATH, RegionData.class);
            } catch (Exception e) {
                logger.error("Could not load custom region config from file", e);
            }
        }
        return loadYaml(
                Paths.get(ClassLoader.getSystemResource("config/region-data.yaml").toURI()),
                RegionData.class);
    }
}
