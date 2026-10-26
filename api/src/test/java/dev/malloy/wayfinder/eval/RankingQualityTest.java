package dev.malloy.wayfinder.eval;

import static org.assertj.core.api.Assertions.assertThat;

import dev.malloy.wayfinder.domain.Destination;
import dev.malloy.wayfinder.domain.DestinationCatalog;
import dev.malloy.wayfinder.domain.DestinationScorer;
import dev.malloy.wayfinder.domain.ScoredDestination;
import dev.malloy.wayfinder.support.PostgresTestBase;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Measures how good the ranking actually is, against hand-labelled relevance judgments.
 *
 * This is the only gate in the project whose number can get genuinely worse when the code
 * changes. A status-code assertion cannot regress in a way that means anything; a ranking
 * metric can, and that is the point of it existing.
 *
 * Evaluated against the real seeded catalog rather than a fixture, because the backlog
 * states its thresholds against that dataset and a ranking measured on different data
 * would be measuring a different system.
 *
 * Carries T-EVAL-001 and T-EVAL-002.
 */
@SpringBootTest
class RankingQualityTest extends PostgresTestBase {

    /** Ten, because a traveller scanning a list does not scroll far. */
    private static final int K = 10;

    private static EvaluationSet evaluation;

    @Autowired
    private DestinationCatalog catalog;

    @BeforeAll
    static void loadEvaluationSet() {
        evaluation = EvaluationSet.load();
    }

    @Test
    @DisplayName("Every judged destination exists in the catalog")
    void judgmentsReferenceRealDestinations() {
        List<String> known = catalog.findAll().stream().map(Destination::slug).toList();

        // A typo in the judgments file would silently lower every score, because an
        // unknown slug simply never appears in a ranking. That would look like a ranking
        // regression and waste an afternoon.
        for (Map.Entry<String, Map<String, Integer>> entry : evaluation.judgments().entrySet()) {
            assertThat(known)
                .as("judgments for persona %s reference destinations that exist", entry.getKey())
                .containsAll(entry.getValue().keySet());
        }
    }

    @Test
    @DisplayName("T-EVAL-001: personalized ranking beats an unpersonalized baseline")
    void personalizationBeatsTheBaseline() {
        List<Destination> all = catalog.findAll();

        Map<String, Double> personalized = new LinkedHashMap<>();
        Map<String, Double> baseline = new LinkedHashMap<>();

        for (EvaluationSet.Persona persona : evaluation.personas()) {
            Map<String, Integer> judgments = evaluation.judgmentsFor(persona.name());

            List<String> ranked = DestinationScorer.rank(all, persona.preferences())
                .stream().map(ScoredDestination::slug).toList();

            // The honest "no ranking at all" comparison: the catalog in its natural order,
            // which is what the unpersonalized endpoint returns today.
            List<String> unranked = all.stream().map(Destination::slug).toList();

            personalized.put(persona.name(), RankingMetrics.ndcgAt(ranked, judgments, K));
            baseline.put(persona.name(), RankingMetrics.ndcgAt(unranked, judgments, K));
        }

        double meanPersonalized = mean(personalized.values());
        double meanBaseline = mean(baseline.values());

        report(personalized, baseline, meanPersonalized, meanBaseline);

        assertThat(meanPersonalized)
            .as("mean NDCG@%d must beat the unpersonalized catalog order, "
                + "or the scoring is decoration", K)
            .isGreaterThan(meanBaseline);

        // Per persona as well as on average: a scorer that is excellent for one traveller
        // and worse than nothing for another has a mean that hides the problem.
        for (EvaluationSet.Persona persona : evaluation.personas()) {
            assertThat(personalized.get(persona.name()))
                .as("NDCG@%d for persona %s must beat the baseline", K, persona.name())
                .isGreaterThan(baseline.get(persona.name()));
        }
    }

    @Test
    @DisplayName("T-EVAL-002: ranking quality does not regress below the measured floor")
    void rankingQualityDoesNotRegress() {
        List<Destination> all = catalog.findAll();

        double mean = mean(evaluation.personas().stream()
            .map(persona -> RankingMetrics.ndcgAt(
                DestinationScorer.rank(all, persona.preferences())
                    .stream().map(ScoredDestination::slug).toList(),
                evaluation.judgmentsFor(persona.name()), K))
            .toList());

        assertThat(mean)
            .as("mean NDCG@%d across all personas", K)
            .isGreaterThanOrEqualTo(MEASURED_FLOOR);
    }

    /**
     * The floor this ranking is held to.
     *
     * Measured, not chosen. The scorer achieves a mean NDCG@10 of 0.9469 against the
     * judgments in src/test/resources/eval/judgments.csv, and this is that figure rounded
     * down to two decimal places. The measurement is deterministic, so the small margin is
     * for rounding rather than for variance.
     *
     * Raising this is a deliberate act that should accompany an improvement to the
     * scoring. Lowering it is an admission that should not pass review quietly.
     */
    private static final double MEASURED_FLOOR = 0.94;

    private static double mean(Iterable<Double> values) {
        double sum = 0.0;
        int count = 0;
        for (double value : values) {
            sum += value;
            count++;
        }
        if (count == 0) {
            throw new IllegalStateException("no personas to average over");
        }
        return sum / count;
    }

    private static void report(Map<String, Double> personalized, Map<String, Double> baseline,
                               double meanPersonalized, double meanBaseline) {
        System.out.printf("%n  ranking quality, NDCG@%d%n", K);
        System.out.printf("  %-22s %10s %10s %9s%n", "persona", "ranked", "baseline", "delta");
        for (Map.Entry<String, Double> entry : personalized.entrySet()) {
            double base = baseline.get(entry.getKey());
            System.out.printf("  %-22s %10.4f %10.4f %+9.4f%n",
                entry.getKey(), entry.getValue(), base, entry.getValue() - base);
        }
        System.out.printf("  %-22s %10.4f %10.4f %+9.4f%n%n",
            "MEAN", meanPersonalized, meanBaseline, meanPersonalized - meanBaseline);
    }
}
