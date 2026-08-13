import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

subprojects {
    apply(plugin = "java")
    apply(plugin = "checkstyle")

    group = "s.reports"
    version = "0.1.0"

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    extensions.configure<CheckstyleExtension> {
        toolVersion = "13.10.0"
        configFile = rootProject.file("checkstyle/checkstyle.xml")
        isIgnoreFailures = false
        sourceSets = listOf(extensions.getByType<SourceSetContainer>().getByName("main"))
    }
}
