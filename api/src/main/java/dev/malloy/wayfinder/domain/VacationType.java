package dev.malloy.wayfinder.domain;

/**
 * The kind of trip a destination offers. US-04 filters on this.
 *
 * Stored as text in the database rather than by ordinal, so inserting a value in the
 * middle of this list later cannot silently reinterpret existing rows.
 */
public enum VacationType {

    /**
     * Detox and wellness travel. Present because the originating project pivoted the whole
     * product to this category mid-sprint, which is the change the retrospective examines.
     */
    WELLNESS,

    BEACH,
    ADVENTURE,
    CITY,
    NATURE,
    CULTURAL
}
