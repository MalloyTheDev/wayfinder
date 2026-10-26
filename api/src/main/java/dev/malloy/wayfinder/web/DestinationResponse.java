package dev.malloy.wayfinder.web;

import dev.malloy.wayfinder.domain.Destination;

/**
 * What a destination looks like on the wire.
 *
 * Separate from the domain record so that the published contract and the internal model
 * can move independently. Renaming a domain field should be a decision about the code,
 * not an unannounced breaking change for every client.
 *
 * @param slug         stable identifier, safe to use in a URL and in a bookmark
 * @param name         display name
 * @param region       broad geographic area
 * @param vacationType the kind of trip, which US-04 filters on
 * @param priceBand    affordability bracket
 * @param summary      one line, never longer than 140 characters (AC-01.4)
 */
record DestinationResponse(
    String slug,
    String name,
    String region,
    String vacationType,
    String priceBand,
    String summary
) {

    static DestinationResponse from(Destination destination) {
        return new DestinationResponse(
            destination.slug(),
            destination.name(),
            destination.region().name(),
            destination.vacationType().name(),
            destination.priceBand().name(),
            destination.summary()
        );
    }
}
