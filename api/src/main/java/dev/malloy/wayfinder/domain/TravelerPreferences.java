package dev.malloy.wayfinder.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * What a traveller has told us about the trips they want.
 *
 * An empty set means "no constraint" rather than "nothing matches". A traveller who has
 * not picked a region should not have every destination scored down for it; they should
 * simply be scored on the dimensions they did express. US-02's AC-02.2 requires that no
 * preference is chosen on the user's behalf, so an unset preference has to mean something
 * sensible rather than being impossible to represent.
 *
 * @param vacationTypes kinds of trip the traveller is interested in, empty for any
 * @param regions       areas the traveller will travel to, empty for anywhere
 * @param budget        the affordability bracket they are aiming at, never null
 */
public record TravelerPreferences(
    Set<VacationType> vacationTypes,
    Set<Region> regions,
    PriceBand budget
) {

    public TravelerPreferences {
        if (budget == null) {
            throw new IllegalArgumentException("budget must be present");
        }
        // Defensive copies into enum sets: the preferences travel through scoring, and a
        // caller mutating the set afterwards would make a ranking irreproducible, which
        // AC-03.5 forbids.
        vacationTypes = immutableCopy(vacationTypes, VacationType.class);
        regions = immutableCopy(regions, Region.class);
    }

    private static <E extends Enum<E>> Set<E> immutableCopy(Set<E> source, Class<E> type) {
        if (source == null || source.isEmpty()) {
            return Collections.unmodifiableSet(EnumSet.noneOf(type));
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(source));
    }

    /** A traveller who expressed only a budget. Used as the unpersonalized comparison. */
    public static TravelerPreferences onlyBudget(PriceBand budget) {
        return new TravelerPreferences(Set.of(), Set.of(), budget);
    }
}
