package dev.malloy.wayfinder.domain;

import java.util.Comparator;
import java.util.List;

/**
 * A destination with its match score and the reasoning behind it.
 *
 * @param destination   the destination scored
 * @param score         overall match, 0.0 to 1.0, the sum of the factor contributions
 * @param contributions each factor's share, summing to score
 */
public record ScoredDestination(
    Destination destination,
    double score,
    List<FactorContribution> contributions
) {

    /**
     * The order a ranked list is presented in: best match first, slug ascending to break ties.
     *
     * The tiebreaker is not cosmetic. Without it two destinations with identical scores
     * could come back in either order depending on the input order, which would break
     * AC-03.5 (reproducible) and AC-03.6 (order-independent). Sorting descending by score
     * also makes AC-03.2 hold for every possible cutoff by construction, rather than for
     * the one cutoff someone happened to pick.
     */
    public static final Comparator<ScoredDestination> RANKING_ORDER =
        Comparator.comparingDouble(ScoredDestination::score).reversed()
            .thenComparing(scored -> scored.destination().slug());

    public ScoredDestination {
        if (destination == null) {
            throw new IllegalArgumentException("destination must be present");
        }
        if (!(score >= 0.0 && score <= 1.0)) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0, got " + score);
        }
        contributions = List.copyOf(contributions);
    }

    public String slug() {
        return destination.slug();
    }
}
