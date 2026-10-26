package dev.malloy.wayfinder.domain;

/**
 * One factor's share of a destination's match score.
 *
 * Exists so a score can be explained rather than merely produced, which is AC-03.4. A
 * ranking that cannot say why it ranked something is not reviewable, and "the algorithm
 * decided" is not an answer anyone can act on.
 *
 * @param factor       what was compared, for example "vacation type"
 * @param weight       how much this factor is worth, 0.0 to 1.0, across factors summing to 1.0
 * @param match        how well the destination matched on this factor alone, 0.0 to 1.0
 * @param contribution weight multiplied by match, which is this factor's share of the total
 */
public record FactorContribution(String factor, double weight, double match, double contribution) {

    public FactorContribution {
        if (factor == null || factor.isBlank()) {
            throw new IllegalArgumentException("factor must be named");
        }
        requireUnitInterval(weight, "weight");
        requireUnitInterval(match, "match");
        requireUnitInterval(contribution, "contribution");
    }

    static FactorContribution of(String factor, double weight, double match) {
        return new FactorContribution(factor, weight, match, weight * match);
    }

    private static void requireUnitInterval(double value, String name) {
        if (!(value >= 0.0 && value <= 1.0)) {
            throw new IllegalArgumentException(name + " must be between 0.0 and 1.0, got " + value);
        }
    }
}
