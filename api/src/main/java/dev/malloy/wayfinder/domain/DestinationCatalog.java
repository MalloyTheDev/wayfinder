package dev.malloy.wayfinder.domain;

import java.util.List;
import java.util.Optional;

/**
 * Read access to the destination catalog, expressed in domain terms.
 *
 * The domain declares what it needs and the persistence package supplies it, rather than
 * the domain reaching for a repository. That is what lets US-03's ranking be tested
 * against a handful of in-memory destinations with no database running, which matters
 * because AC-03.2 and AC-03.5 are assertions about ordering, not about storage.
 */
public interface DestinationCatalog {

    /**
     * Every destination, in a stable order.
     *
     * Ordering is part of the contract rather than an accident of the query plan, because
     * AC-01.6 requires that identical inputs produce an identical ordering. Implementations
     * order by slug, which is unique, so the result can never depend on insertion order or
     * on how the database happened to scan the table.
     */
    List<Destination> findAll();

    /** One destination by its stable identifier, or empty when no such destination exists. */
    Optional<Destination> findBySlug(String slug);
}
