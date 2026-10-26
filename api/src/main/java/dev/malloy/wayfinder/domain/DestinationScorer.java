package dev.malloy.wayfinder.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Scores destinations against a traveller's stated preferences, and ranks them.
 *
 * No Spring, no JPA, no database. That is what lets the ordering invariants in US-03 be
 * tested directly against a handful of in-memory destinations, and ArchitectureTest
 * enforces it rather than trusting this comment.
 *
 * The weights below are a judgment, not a measurement, and the backlog says so: AC-03.1's
 * cutoff carries a `provisional` basis. What is *not* a judgment is the ordering behaviour
 * this class guarantees, which is why US-03's criteria are expressed as invariants that
 * hold whatever the weights are. Retune the weights and AC-03.2 through AC-03.6 still
 * hold; only the ranking quality measured by the evaluation harness moves.
 */
public final class DestinationScorer {

    /**
     * AC-03.1. A destination scoring below this is "unlikely" for this traveller.
     *
     * Deliberately not used for filtering anywhere in this class. AC-03.2 is stated to
     * hold for every cutoff, so the ranking must not depend on this particular number;
     * it exists for callers that want to label or hide poor matches.
     */
    public static final double UNLIKELY_BELOW = 0.25;

    /**
     * Weights sum to exactly 1.0, which is what keeps a score inside 0.0 to 1.0 and makes
     * the factor contributions sum to the total (AC-03.4).
     */
    private static final double VACATION_TYPE_WEIGHT = 0.5;
    private static final double REGION_WEIGHT = 0.2;
    private static final double PRICE_WEIGHT = 0.3;

    private DestinationScorer() {
    }

    /**
     * Rank destinations best match first.
     *
     * The returned order is total: ties are broken by slug, so the result depends only on
     * the set of destinations and the preferences, never on the order they arrived in.
     */
    public static List<ScoredDestination> rank(
        List<Destination> destinations, TravelerPreferences preferences) {

        List<ScoredDestination> scored = new ArrayList<>(destinations.size());
        for (Destination destination : destinations) {
            scored.add(score(destination, preferences));
        }
        scored.sort(ScoredDestination.RANKING_ORDER);
        return List.copyOf(scored);
    }

    /** Score one destination, recording what each factor contributed. */
    public static ScoredDestination score(Destination destination, TravelerPreferences preferences) {
        if (destination == null || preferences == null) {
            throw new IllegalArgumentException("destination and preferences must be present");
        }

        List<FactorContribution> contributions = List.of(
            FactorContribution.of("vacation type", VACATION_TYPE_WEIGHT,
                membership(preferences.vacationTypes(), destination.vacationType())),
            FactorContribution.of("region", REGION_WEIGHT,
                membership(preferences.regions(), destination.region())),
            FactorContribution.of("price band", PRICE_WEIGHT,
                priceProximity(preferences.budget(), destination.priceBand())));

        double total = 0.0;
        for (FactorContribution contribution : contributions) {
            total += contribution.contribution();
        }

        // Floating point summation of three weights that sum to 1.0 can land a hair
        // outside the interval. Clamping keeps the ScoredDestination invariant true
        // without pretending the arithmetic was exact.
        return new ScoredDestination(destination, clampToUnitInterval(total), contributions);
    }

    /**
     * An unexpressed preference is not a failed one.
     *
     * An empty set means the traveller stated no constraint on this dimension, so every
     * destination matches it fully. Scoring it as zero would punish travellers for
     * leaving a control unset, which is exactly the state AC-02.2 requires controls to
     * start in.
     */
    private static <E extends Enum<E>> double membership(java.util.Set<E> preferred, E actual) {
        if (preferred.isEmpty()) {
            return 1.0;
        }
        return preferred.contains(actual) ? 1.0 : 0.0;
    }

    /**
     * How close a destination's price band is to the traveller's budget, on 0.0 to 1.0.
     *
     * Graded rather than binary: someone budgeting for MODERATE is better served by a
     * PREMIUM destination than by a LUXURY one, and a binary match would rank those two
     * identically.
     */
    private static double priceProximity(PriceBand budget, PriceBand actual) {
        int distance = budget.distanceTo(actual);
        return 1.0 - ((double) distance / PriceBand.maximumDistance());
    }

    private static double clampToUnitInterval(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }
}
