package dev.malloy.wayfinder.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The US-03 ordering invariants, tested with no Spring and no database.
 *
 * These are invariants rather than thresholds, which is the distinction the backlog now
 * turns on. Every assertion here holds whatever the scoring weights are. Retune the
 * weights and these still pass; only the measured ranking quality in RankingQualityTest
 * moves. That is what makes them immune to the obvious criticism of a self-set number.
 *
 * Carries T-RANK-001 through T-RANK-006.
 */
class DestinationScorerTest {

    private static final List<Destination> CATALOG = List.of(
        destination("kyoto-retreat", Region.ASIA, VacationType.WELLNESS, PriceBand.PREMIUM),
        destination("rishikesh-ashram", Region.ASIA, VacationType.WELLNESS, PriceBand.BUDGET),
        destination("algarve-coast", Region.EUROPE, VacationType.BEACH, PriceBand.MODERATE),
        destination("maldives-atoll", Region.ASIA, VacationType.BEACH, PriceBand.LUXURY),
        destination("patagonia-trek", Region.SOUTH_AMERICA, VacationType.ADVENTURE, PriceBand.PREMIUM),
        destination("moab-canyons", Region.NORTH_AMERICA, VacationType.ADVENTURE, PriceBand.MODERATE),
        destination("lisbon-break", Region.EUROPE, VacationType.CITY, PriceBand.MODERATE),
        destination("marrakech-medina", Region.AFRICA, VacationType.CULTURAL, PriceBand.BUDGET),
        destination("banff-lakes", Region.NORTH_AMERICA, VacationType.NATURE, PriceBand.PREMIUM),
        destination("byron-bay", Region.OCEANIA, VacationType.WELLNESS, PriceBand.MODERATE));

    private static final TravelerPreferences WELLNESS_ON_A_BUDGET = new TravelerPreferences(
        Set.of(VacationType.WELLNESS), Set.of(), PriceBand.BUDGET);

    private static final TravelerPreferences LUXURY_BEACH_IN_ASIA = new TravelerPreferences(
        Set.of(VacationType.BEACH), Set.of(Region.ASIA), PriceBand.LUXURY);

    private static Destination destination(
        String slug, Region region, VacationType type, PriceBand band) {
        return new Destination(slug, slug, region, type, band, "A summary for " + slug + ".");
    }

    private static List<String> slugsOf(List<ScoredDestination> ranked) {
        return ranked.stream().map(ScoredDestination::slug).toList();
    }

    @Test
    @DisplayName("T-RANK-001: no destination below a cutoff outranks one at or above it, "
        + "for every cutoff (AC-03.2)")
    void noInversionsAtAnyCutoff() {
        List<ScoredDestination> ranked = DestinationScorer.rank(CATALOG, WELLNESS_ON_A_BUDGET);

        // The criterion is stated for any cutoff, so the test sweeps the whole interval
        // rather than checking the one value the scorer happens to expose. 101 cutoffs at
        // 0.01 resolution: a violation would have to hide inside a hundredth to escape.
        for (int step = 0; step <= 100; step++) {
            double cutoff = step / 100.0;
            int inversions = 0;
            for (int i = 0; i < ranked.size(); i++) {
                for (int j = i + 1; j < ranked.size(); j++) {
                    boolean earlierIsBelow = ranked.get(i).score() < cutoff;
                    boolean laterIsAtOrAbove = ranked.get(j).score() >= cutoff;
                    if (earlierIsBelow && laterIsAtOrAbove) {
                        inversions++;
                    }
                }
            }
            assertThat(inversions)
                .as("inversions at cutoff %.2f", cutoff)
                .isZero();
        }
    }

    @Test
    @DisplayName("T-RANK-002: per-factor contributions sum to the total score within 0.001 "
        + "(AC-03.4)")
    void contributionsExplainTheScore() {
        for (Destination destination : CATALOG) {
            ScoredDestination scored = DestinationScorer.score(destination, WELLNESS_ON_A_BUDGET);

            double summed = scored.contributions().stream()
                .mapToDouble(FactorContribution::contribution)
                .sum();

            assertThat(summed)
                .as("contributions for %s must explain its score", destination.slug())
                .isCloseTo(scored.score(), org.assertj.core.data.Offset.offset(0.001));

            assertThat(scored.contributions())
                .as("every factor is named and weighted")
                .isNotEmpty()
                .allSatisfy(factor -> {
                    assertThat(factor.factor()).isNotBlank();
                    assertThat(factor.contribution())
                        .isCloseTo(factor.weight() * factor.match(),
                            org.assertj.core.data.Offset.offset(1e-12));
                });
        }
    }

    @Test
    @DisplayName("T-RANK-003: 100 identical calls produce 1 distinct ordering (AC-03.5)")
    void rankingIsReproducible() {
        long distinct = IntStream.range(0, 100)
            .mapToObj(i -> slugsOf(DestinationScorer.rank(CATALOG, WELLNESS_ON_A_BUDGET)))
            .distinct()
            .count();

        assertThat(distinct).isEqualTo(1);
    }

    @Test
    @DisplayName("T-RANK-004: shuffling the input 100 times produces 1 distinct ordering "
        + "(AC-03.6)")
    void rankingIsOrderIndependent() {
        // Seeded so a failure is reproducible. An unseeded shuffle would make this test
        // fail on some runs and not others, which is the behaviour it exists to forbid.
        Random random = new Random(20261026L);
        List<String> expected = slugsOf(DestinationScorer.rank(CATALOG, WELLNESS_ON_A_BUDGET));

        for (int attempt = 0; attempt < 100; attempt++) {
            List<Destination> shuffled = new ArrayList<>(CATALOG);
            Collections.shuffle(shuffled, random);

            assertThat(slugsOf(DestinationScorer.rank(shuffled, WELLNESS_ON_A_BUDGET)))
                .as("ranking must not depend on the order destinations arrived in")
                .isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("T-RANK-005: two personas receive orderings differing by 1 or more positions "
        + "(AC-03.3)")
    void personalizationChangesTheResult() {
        List<String> wellness = slugsOf(DestinationScorer.rank(CATALOG, WELLNESS_ON_A_BUDGET));
        List<String> beach = slugsOf(DestinationScorer.rank(CATALOG, LUXURY_BEACH_IN_ASIA));

        long moved = IntStream.range(0, wellness.size())
            .filter(i -> !wellness.get(i).equals(beach.get(i)))
            .count();

        assertThat(moved)
            .as("a personalization that returns the same order for every traveller "
                + "is not personalizing")
            .isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("T-RANK-006: every score lies within 0.0 to 1.0, and the ranking is sorted")
    void scoresAreBoundedAndSorted() {
        List<ScoredDestination> ranked = DestinationScorer.rank(CATALOG, LUXURY_BEACH_IN_ASIA);

        assertThat(ranked).allSatisfy(scored ->
            assertThat(scored.score()).isBetween(0.0, 1.0));

        // Sorted descending by score is what makes T-RANK-001 hold by construction rather
        // than by luck, so it is worth asserting directly.
        for (int i = 1; i < ranked.size(); i++) {
            assertThat(ranked.get(i - 1).score())
                .as("position %d must not score below position %d", i - 1, i)
                .isGreaterThanOrEqualTo(ranked.get(i).score());
        }
    }
}
