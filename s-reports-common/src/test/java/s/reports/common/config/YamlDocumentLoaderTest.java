package s.reports.common.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlDocumentLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void unparseableFileReturnsParseFailure() throws IOException {
        final Path file = tempDir.resolve("config.yml");
        Files.writeString(file, "key: [unclosed");

        final YamlLoadResult result = YamlDocumentLoader.load(file, "config.yml", getClass().getClassLoader());

        final YamlLoadResult.ParseFailure failure = assertInstanceOf(YamlLoadResult.ParseFailure.class, result);
        assertTrue(failure.fileName().equals("config.yml"));
    }

    @Test
    void missingFileIsWrittenFromPackagedDefaultAndNeverOverwritten() throws IOException {
        final Path file = tempDir.resolve("test-config.yml");

        final YamlLoadResult first = YamlDocumentLoader.load(file, "test-default.yml", getClass().getClassLoader());
        assertInstanceOf(YamlLoadResult.Loaded.class, first);
        assertTrue(Files.exists(file));

        Files.writeString(file, "operator: value\n");
        final YamlLoadResult second = YamlDocumentLoader.load(file, "test-default.yml", getClass().getClassLoader());

        final YamlLoadResult.Loaded loaded = assertInstanceOf(YamlLoadResult.Loaded.class, second);
        assertTrue(loaded.root().containsKey("operator"));
    }
}
