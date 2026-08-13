package s.reports.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public final class YamlDocumentLoader {

    private YamlDocumentLoader() {
    }

    public static YamlLoadResult load(Path targetFile, String classpathResource, ClassLoader resourceLoader) {
        try {
            if (Files.notExists(targetFile)) {
                writePackagedDefault(targetFile, classpathResource, resourceLoader);
            }
            try (InputStream input = Files.newInputStream(targetFile)) {
                final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                final Object loaded = yaml.load(input);
                return new YamlLoadResult.Loaded(asMap(loaded));
            }
        } catch (YAMLException | IOException exception) {
            return new YamlLoadResult.ParseFailure(targetFile.getFileName().toString(), String.valueOf(exception.getMessage()));
        }
    }

    private static void writePackagedDefault(Path targetFile, String classpathResource, ClassLoader resourceLoader)
            throws IOException {
        final Path parent = targetFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (InputStream defaultStream = resourceLoader.getResourceAsStream(classpathResource)) {
            if (defaultStream == null) {
                throw new IOException("Missing packaged default resource " + classpathResource);
            }
            Files.copy(defaultStream, targetFile);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object loaded) {
        return loaded instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
