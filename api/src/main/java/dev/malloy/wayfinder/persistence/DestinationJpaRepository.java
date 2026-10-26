package dev.malloy.wayfinder.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data access to the destinations table.
 *
 * Package-private, and never injected outside this package. Callers depend on the
 * DestinationCatalog port in the domain instead, which is what keeps Spring Data out of
 * the ranking code that US-03 has to test without a database.
 */
interface DestinationJpaRepository extends JpaRepository<DestinationEntity, Long> {

    /**
     * Every destination ordered by slug.
     *
     * The order is explicit because AC-01.6 requires identical inputs to produce an
     * identical ordering. Without an ORDER BY, Postgres is free to return rows in whatever
     * order the scan produced, which is stable in practice right up until it is not.
     */
    List<DestinationEntity> findAllByOrderBySlugAsc();

    Optional<DestinationEntity> findBySlug(String slug);
}
