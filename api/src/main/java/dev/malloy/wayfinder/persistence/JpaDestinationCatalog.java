package dev.malloy.wayfinder.persistence;

import dev.malloy.wayfinder.domain.Destination;
import dev.malloy.wayfinder.domain.DestinationCatalog;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supplies the domain's {@link DestinationCatalog} port from Postgres.
 *
 * This is the only class that knows both halves: the domain types above it and Spring Data
 * below it. Everything else depends on the port.
 */
@Repository
class JpaDestinationCatalog implements DestinationCatalog {

    private final DestinationJpaRepository repository;

    JpaDestinationCatalog(DestinationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Destination> findAll() {
        return repository.findAllByOrderBySlugAsc()
            .stream()
            .map(DestinationEntity::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Destination> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return repository.findBySlug(slug.strip()).map(DestinationEntity::toDomain);
    }
}
