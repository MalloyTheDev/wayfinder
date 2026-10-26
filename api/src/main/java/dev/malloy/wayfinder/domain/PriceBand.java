package dev.malloy.wayfinder.domain;

/**
 * Affordability bracket.
 *
 * A bracket rather than a price because a destination has no single price, and because
 * US-03 scores how well a destination matches what a traveller is willing to spend. Four
 * bands are enough to express that and few enough that a traveller can choose between
 * them without guessing.
 *
 * Declared cheapest to most expensive. Unlike the other enums in this package the order
 * here is meaningful, since scoring measures the distance between two bands, so new
 * values must be inserted in the correct position rather than appended.
 */
public enum PriceBand {
    BUDGET,
    MODERATE,
    PREMIUM,
    LUXURY;

    /** How many bands apart two values are, used by US-03 scoring. Never negative. */
    public int distanceTo(PriceBand other) {
        return Math.abs(ordinal() - other.ordinal());
    }

    /** The widest possible distance, so a score can be normalised onto 0.0 to 1.0. */
    public static int maximumDistance() {
        return values().length - 1;
    }
}
