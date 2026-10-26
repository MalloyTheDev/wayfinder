package dev.malloy.wayfinder.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Parses application.yml the way Spring Boot does, and fails loudly when it cannot.
 *
 * This exists because of a specific afternoon. A second `datasource:` key was added under
 * `spring:` while one already existed. Most YAML parsers accept that silently and keep the
 * last value; SnakeYAML with Spring Boot's settings rejects it. The result was every
 * database-backed test failing at once with "Failed to load ApplicationContext" and the
 * actual cause, four words long, buried inside a two-hundred-line context dump.
 *
 * A duplicate key now fails here instead, in milliseconds, naming the key and the line.
 * The lesson generalises: validating configuration with a more permissive parser than the
 * one that will actually read it is not validation.
 */
class ApplicationYamlTest {

    @Test
    @DisplayName("application.yml parses with duplicate keys rejected, as Spring Boot reads it")
    void applicationYamlHasNoDuplicateKeys() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            assertThat(in).as("application.yml must be on the classpath").isNotNull();
            assertThatCode(() -> yaml.load(in)).doesNotThrowAnyException();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("could not read application.yml", e);
        }
    }

    @Test
    @DisplayName("The settings that make a deploy safe are present and not quietly dropped")
    @SuppressWarnings("unchecked")
    void deploymentCriticalSettingsArePresent() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));

        Map<String, Object> root;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            root = yaml.load(in);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("could not read application.yml", e);
        }

        Map<String, Object> spring = (Map<String, Object>) root.get("spring");
        Map<String, Object> server = (Map<String, Object>) root.get("server");

        // Graceful shutdown: without it, a blue-green cutover drops in-flight requests.
        assertThat(server).containsEntry("shutdown", "graceful");

        // Hibernate must never own the schema. If this flips, migrations stop being the
        // source of truth and the CHECK constraints behind AC-01.3 and AC-01.4 vanish.
        Map<String, Object> jpa = (Map<String, Object>) spring.get("jpa");
        Map<String, Object> hibernate = (Map<String, Object>) jpa.get("hibernate");
        assertThat(hibernate).containsEntry("ddl-auto", "none");

        // Flyway must stay overridable, because production turns it off and runs
        // migrations in a separate release step that can fail the deploy.
        Map<String, Object> flyway = (Map<String, Object>) spring.get("flyway");
        assertThat(flyway.get("enabled").toString())
            .as("flyway.enabled must remain environment-overridable")
            .contains("SPRING_FLYWAY_ENABLED");

        // Error responses must never carry a stack trace to a client.
        Map<String, Object> error = (Map<String, Object>) server.get("error");
        assertThat(error).containsEntry("include-stacktrace", "never");
        assertThat(error).containsEntry("include-message", "never");
    }
}
