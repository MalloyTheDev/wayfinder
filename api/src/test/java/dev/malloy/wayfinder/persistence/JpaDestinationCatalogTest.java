package dev.malloy.wayfinder.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.malloy.wayfinder.domain.Destination;
import dev.malloy.wayfinder.support.PostgresTestBase;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * The persistence adapter, against real Postgres.
 *
 * The ordering tests matter more than they look. AC-01.6 and AC-00.2 require that identical
 * inputs produce an identical ordering, and the usual way that breaks is a query with no
 * ORDER BY which happens to return insertion order until the day the plan changes.
 *
 * Carries T-CAT-001 through T-CAT-003.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaDestinationCatalog.class)
class JpaDestinationCatalogTest extends PostgresTestBase {

    @Autowired
    private JpaDestinationCatalog catalog;

    @Test
    @DisplayName("T-CAT-001: findAll is ordered by slug")
    void findAllIsOrderedBySlug() {
        List<Destination> all = catalog.findAll();

        assertThat(all).isNotEmpty();
        assertThat(all)
            .extracting(Destination::slug)
            .isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    @DisplayName("T-CAT-002: repeated calls return one distinct ordering")
    void findAllIsDeterministicAcrossRepeatedCalls() {
        List<String> first = catalog.findAll().stream().map(Destination::slug).toList();

        long distinctOrderings = IntStream.range(0, 25)
            .mapToObj(i -> catalog.findAll().stream().map(Destination::slug).toList())
            .distinct()
            .count();

        assertThat(distinctOrderings)
            .as("25 identical calls must produce exactly 1 ordering")
            .isEqualTo(1);
        assertThat(catalog.findAll().stream().map(Destination::slug).toList())
            .isEqualTo(first);
    }

    @Test
    @DisplayName("T-CAT-003: a known slug returns its destination")
    void findsAKnownSlug() {
        assertThat(catalog.findBySlug("kyoto-mindfulness-retreat"))
            .isPresent()
            .get()
            .satisfies(destination -> {
                assertThat(destination.name()).isEqualTo("Kyoto Mindfulness Retreat");
                assertThat(destination.summary()).isNotBlank();
            });
    }

    @Test
    @DisplayName("An unknown slug is empty rather than an error")
    void unknownSlugIsEmpty() {
        assertThat(catalog.findBySlug("no-such-destination")).isEmpty();
    }

    @Test
    @DisplayName("A null or blank slug is empty rather than a query")
    void blankSlugIsEmptyWithoutQuerying() {
        assertThat(catalog.findBySlug(null)).isEmpty();
        assertThat(catalog.findBySlug("")).isEmpty();
        assertThat(catalog.findBySlug("   ")).isEmpty();
    }

    @Test
    @DisplayName("Surrounding whitespace in a slug is tolerated")
    void slugIsStrippedBeforeLookup() {
        assertThat(catalog.findBySlug("  kyoto-mindfulness-retreat  ")).isPresent();
    }

    @Test
    @DisplayName("Every stored row survives conversion into a valid domain object")
    void everyStoredRowConvertsToAValidDestination() {
        // The Destination constructor re-validates, so this passing means no seeded row
        // violates an invariant the domain cares about.
        assertThat(catalog.findAll()).allSatisfy(destination ->
            assertThat(destination.slug()).isNotBlank());
    }
}
