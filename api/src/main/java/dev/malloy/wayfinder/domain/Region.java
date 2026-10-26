package dev.malloy.wayfinder.domain;

/**
 * Broad geographic area. Coarse on purpose: it exists to filter and to score proximity to
 * a traveller's stated preference, not to be an accurate geography.
 *
 * Stored as text rather than by ordinal, so the order here is not load-bearing.
 */
public enum Region {
    NORTH_AMERICA,
    SOUTH_AMERICA,
    EUROPE,
    AFRICA,
    ASIA,
    OCEANIA
}
