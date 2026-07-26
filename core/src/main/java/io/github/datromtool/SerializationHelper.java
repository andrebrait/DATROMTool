package io.github.datromtool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.config.JsonNodeMerge;
import io.github.datromtool.config.Profile;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.datafile.logiqx.Datafile;
import io.github.datromtool.domain.detector.Detector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.datatype.guava.GuavaModule;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Getter
@RequiredArgsConstructor(access = PRIVATE)
public final class SerializationHelper {

    public static final String GIT_BUILD_VERSION = "git.build.version";
    public static final Path DEFAULT_BASE_PATH = Paths.get(System.getProperty("user.home")).resolve(".DATROMTool");

    private static final Pattern YAML_PATTERN = Pattern.compile("^.+\\.(?:yaml|yml)$", CASE_INSENSITIVE);
    private static final Pattern JSON_PATTERN = Pattern.compile("^.+\\.(?:json|js)$", CASE_INSENSITIVE);

    private final Path detectorsBasePath;
    private final Path appConfigPath;
    private final Path regionDataPath;

    public SerializationHelper(Path basePath) {
        this.detectorsBasePath = basePath.resolve("detectors");
        this.appConfigPath = basePath.resolve("config.yaml");
        this.regionDataPath = basePath.resolve("region-data.yaml");
    }

    private final XmlMapper xmlMapper = createXmlMapper();
    private final JsonMapper jsonMapper = createJsonMapper();
    private final YAMLMapper yamlMapper = createYamlMapper();
    private final Properties gitProps = loadProperties();

    private static Properties loadProperties() {
        Properties gitProps = new Properties();
        URL gitPropertiesResource = ClassLoader.getSystemResource("git.properties");
        if (gitPropertiesResource != null) {
            try (InputStream stream = gitPropertiesResource.openStream()) {
                gitProps.load(stream);
            } catch (IOException e) {
                log.error("Could not load version information", e);
            }
        } else {
            log.error("Could not find 'git.properties'");
        }
        return gitProps;
    }

    public static SerializationHelper getInstance() {
        return SerializationHelperHolder.INSTANCE;
    }

    public static SerializationHelper getInstance(Path basePath) {
        return new SerializationHelper(basePath);
    }

    private final static class SerializationHelperHolder {

        private final static SerializationHelper INSTANCE = new SerializationHelper(DEFAULT_BASE_PATH);
    }

    private static XmlMapper createXmlMapper() {
        return XmlMapper.builder()
                .defaultUseWrapper(false)
                .addModule(new GuavaModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
    }

    private static JsonMapper createJsonMapper() {
        return JsonMapper.builder()
                .addModule(new GuavaModule())
                .addModule(PathJacksonModule.INSTANCE)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
    }

    private static YAMLMapper createYamlMapper() {
        return YAMLMapper.builder()
                .addModule(new GuavaModule())
                .addModule(PathJacksonModule.INSTANCE)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
    }

    public <T> T loadXml(Path xml, Class<T> tClass) throws IOException {
        try (InputStream inputStream = Files.newInputStream(xml)) {
            return loadXml(inputStream, tClass);
        }
    }

    public <T> T loadXml(InputStream inputStream, Class<T> tClass) {
        return xmlMapper.readValue(inputStream, tClass);
    }

    public <T> T loadJsonOrYaml(Path file, Class<T> tClass) throws IOException {
        String fileName = file.getFileName().toString();
        if (JSON_PATTERN.matcher(fileName).matches()) {
            return loadJson(file, tClass);
        } else if (YAML_PATTERN.matcher(fileName).matches()) {
            return loadYaml(file, tClass);
        }
        // We have no idea what this file is, so let's try JSON first and then YAML if it fails
        try {
            return loadJson(file, tClass);
        } catch (JacksonException ignore) {
            return loadYaml(file, tClass);
        }
    }

    /**
     * Loads a single {@link Profile} file (JSON or YAML, dispatched by extension exactly like
     * {@link #loadJsonOrYaml}). Validates the result (see {@link Profile#validate()}) before
     * returning it. Any parse or validation failure is wrapped so the offending file's path is
     * part of the exception message.
     */
    public Profile loadProfile(Path path) throws IOException {
        return toProfile(readProfileTree(path), path);
    }

    /**
     * Loads and layers multiple {@link Profile} files, later files taking precedence over
     * earlier ones. Each file is read to a tree first and merged with {@link JsonNodeMerge}
     * (see {@link Profile}'s Javadoc for the merge rule) before the fully-merged tree is bound
     * to {@link Profile} once, so the result reflects every file's contribution rather than
     * repeated whole-object replacement. An empty list yields a default {@link Profile}.
     */
    public Profile loadProfiles(List<Path> paths) throws IOException {
        if (paths.isEmpty()) {
            Profile profile = Profile.builder().build();
            profile.validate();
            return profile;
        }
        JsonNode merged = null;
        for (Path path : paths) {
            JsonNode node = readProfileTree(path);
            merged = merged == null ? node : JsonNodeMerge.merge(merged, node);
        }
        return toProfile(merged, paths.get(paths.size() - 1));
    }

    private JsonNode readProfileTree(Path path) throws IOException {
        String fileName = path.getFileName().toString();
        try {
            if (JSON_PATTERN.matcher(fileName).matches()) {
                return readTree(path, jsonMapper);
            } else if (YAML_PATTERN.matcher(fileName).matches()) {
                return readTree(path, yamlMapper);
            }
            // We have no idea what this file is, so let's try JSON first and then YAML if it fails.
            // Each attempt re-opens the file: a mapper that fails partway through may have already
            // consumed part of a shared stream, so a fresh stream per attempt is required.
            try {
                return readTree(path, jsonMapper);
            } catch (JacksonException ignore) {
                return readTree(path, yamlMapper);
            }
        } catch (JacksonException e) {
            throw new IOException("Failed to parse profile file '" + path + "': " + e.getMessage(), e);
        }
    }

    private static JsonNode readTree(Path path, ObjectMapper mapper) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return mapper.readTree(inputStream);
        }
    }

    private Profile toProfile(JsonNode node, Path path) throws IOException {
        try {
            Profile profile = jsonMapper.treeToValue(node, Profile.class);
            profile.validate();
            return profile;
        } catch (JacksonException e) {
            throw new IOException("Failed to load profile from '" + path + "': " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid profile in '" + path + "': " + e.getMessage(), e);
        }
    }

    public <T> T loadJson(Path json, Class<T> tClass) throws IOException {
        try (InputStream inputStream = Files.newInputStream(json)) {
            return loadJson(inputStream, tClass);
        }
    }

    public <T> T loadJson(InputStream inputStream, Class<T> tClass) {
        return jsonMapper.readValue(inputStream, tClass);
    }

    public <T> T loadYaml(Path yaml, Class<T> tClass) throws IOException {
        try (InputStream inputStream = Files.newInputStream(yaml)) {
            return loadYaml(inputStream, tClass);
        }
    }

    public <T> T loadYaml(InputStream inputStream, Class<T> tClass) {
        return yamlMapper.readValue(inputStream, tClass);
    }

    public RegionData loadRegionData(Path path) throws IOException {
        return loadYaml(path, RegionData.class);
    }

    public RegionData loadRegionData(URL url) throws IOException {
        try (InputStream stream = url.openStream()) {
            return loadYaml(stream, RegionData.class);
        }
    }

    public RegionData loadRegionData() throws IOException {
        if (Files.isRegularFile(regionDataPath)) {
            try {
                return loadRegionData(regionDataPath);
            } catch (Exception e) {
                log.error("Could not load custom region config from file", e);
            }
        }
        return loadRegionData(ClassLoader.getSystemResource("region-data.yaml"));
    }

    public Detector loadDetector(Path path) throws IOException {
        return loadXml(path, Detector.class);
    }

    public Detector loadDetector(URL url) throws IOException {
        try (InputStream stream = url.openStream()) {
            return loadXml(stream, Detector.class);
        }
    }

    public Detector loadDetector(String name) throws IOException {
        Path path = detectorsBasePath.resolve(name);
        if (Files.isRegularFile(path)) {
            try {
                return loadDetector(path);
            } catch (Exception e) {
                log.error("Could not load detector from file", e);
            }
        }
        return loadDetector(ClassLoader.getSystemResource("detectors/" + name));
    }

    public AppConfig loadAppConfig(Path path) throws IOException {
        return loadYaml(path, AppConfig.class);
    }

    public AppConfig loadAppConfig(URL url) throws IOException {
        try (InputStream stream = url.openStream()) {
            return loadYaml(stream, AppConfig.class);
        }
    }

    public AppConfig loadAppConfig() {
        if (Files.isRegularFile(appConfigPath)) {
            try {
                return loadAppConfig(appConfigPath);
            } catch (Exception e) {
                log.error("Could not load application configuration from file", e);
            }
        }
        return AppConfig.builder().build();
    }

    public ImmutableList<String> writeAsXml(Object object) throws JacksonException {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        builder.add("<?xml version=\"1.0\"?>");
        if (object instanceof Datafile) {
            builder.add("<!DOCTYPE datafile PUBLIC \"-//Logiqx//DTD ROM Management Datafile//EN\" \"http://www.logiqx.com/Dats/datafile.dtd\">");
        }
        builder.add(String.format("<!-- Generated by DATROMTool v%s -->", getVersionString()));
        builder.add(xmlMapper.writeValueAsString(object));
        return builder.build();
    }

    public ImmutableList<String> writeAsJson(Object object) throws JacksonException {
        return ImmutableList.of(jsonMapper.writeValueAsString(object));
    }

    public ImmutableList<String> writeAsYaml(Object object) throws JacksonException {
        return ImmutableList.of(
                String.format("# Generated by DATROMTool v%s", getVersionString()),
                yamlMapper.writeValueAsString(object));
    }

    @Nullable
    public String getVersionString() {
        return gitProps.getProperty(GIT_BUILD_VERSION);
    }
}
