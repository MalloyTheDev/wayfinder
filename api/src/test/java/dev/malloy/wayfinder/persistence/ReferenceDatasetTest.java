package dev.malloy.wayfinder.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.malloy.wayfinder.domain.Destination;
import dev.malloy.wayfinder.support.PostgresTestBase;
import dev.malloy.wayfinder.domain.Region;
import dev.malloy.wayfinder.domain.VacationType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The reference dataset, checked against the real schema after Flyway has run.
 *
 * Every threshold in docs/backlog.md is stated against this data, so these assertions are
 * load-bearing for criteria that have not been written yet: a latency budget measured
 * against a catalog of unknown size means nothing.
 *
 * Carries T-DATA-001 (AC-01.3) and T-DATA-002 (AC-01.4).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaDestinationCatalog.class)
class ReferenceDatasetTest extends PostgresTestBase {

    /** The seeded catalog size. Other milestones state thresholds against this number. */
    private static final int EXPECTED_DESTINATIONS = 30;

    @Autowired
    private JpaDestinationCatalog catalog;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Flyway seeded exactly the expected number of destinations")
    void seedsTheExpectedCatalogSize() {
        assertThat(catalog.findAll()).hasSize(EXPECTED_DESTINATIONS);
    }

    @Test
    @DisplayName("T-DATA-001: every destination carries all four required fields (AC-01.3)")
    void everyDestinationCarriesTheFieldsATravellerNeeds() {
        List<Destination> all = catalog.findAll();

        assertThat(all).isNotEmpty();
        assertThat(all).allSatisfy(destination -> {
            assertThat(destination.name()).isNotBlank();
            assertThat(destination.vacationType()).isNotNull();
            assertThat(destination.region()).isNotNull();
            assertThat(destination.summary()).isNotBlank();
        });
    }

    @Test
    @DisplayName("T-DATA-002: every summary is between 1 and 140 characters (AC-01.4)")
    void everySummaryIsWithinTheLengthBound() {
        assertThat(catalog.findAll()).allSatisfy(destination ->
            assertThat(destination.summary().length())
                .as("summary length for %s", destination.slug())
                .isBetween(1, Destination.SUMMARY_MAX_LENGTH));
    }

    @Test
    @DisplayName("The database enforces the length bound, not only the domain")
    void databaseRejectsAnOverlongSummary() {
        String tooLong = "x".repeat(Destination.SUMMARY_MAX_LENGTH + 1);

        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO destinations (slug, name, region, vacation_type, price_band, summary)
            VALUES ('too-long', 'Too Long', 'ASIA', 'WELLNESS', 'BUDGET', ?)
            """, tooLong))
            .hasMessageContaining("destinations_summary_length");
    }

    @Test
    @DisplayName("The database rejects a vacation type the enum does not define")
    void databaseRejectsAnUnknownVacationType() {
        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO destinations (slug, name, region, vacation_type, price_band, summary)
            VALUES ('bad-type', 'Bad Type', 'ASIA', 'SPELUNKING', 'BUDGET', 'A summary.')
            """))
            .hasMessageContaining("destinations_vacation_type_known");
    }

    @Test
    @DisplayName("Slugs are unique, since they are the public identifier")
    void slugsAreUnique() {
        List<String> slugs = catalog.findAll().stream().map(Destination::slug).toList();
        assertThat(slugs).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Every vacation type and region is represented, so later filters have data")
    void coversEveryVacationTypeAndRegion() {
        List<Destination> all = catalog.findAll();

        assertThat(all).extracting(Destination::vacationType)
            .containsAll(List.of(VacationType.values()));
        assertThat(all).extracting(Destination::region)
            .containsAll(List.of(Region.values()));
    }
}
