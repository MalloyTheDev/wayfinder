package dev.malloy.wayfinder.persistence;

import dev.malloy.wayfinder.domain.Destination;
import dev.malloy.wayfinder.domain.PriceBand;
import dev.malloy.wayfinder.domain.Region;
import dev.malloy.wayfinder.domain.VacationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Storage shape for a destination.
 *
 * Kept separate from the {@link Destination} record on purpose. The domain type carries
 * the invariants and is what the rest of the application reasons about; this class exists
 * only to satisfy JPA, which needs a mutable no-argument-constructible bean with a
 * surrogate key. Merging the two would drag Hibernate into the domain and force the
 * database's surrogate id into code that has no use for it.
 */
@Entity
@Table(name = "destinations")
class DestinationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    /**
     * Stored by name rather than ordinal. An ordinal would reinterpret every existing row
     * if a value were ever inserted into the middle of the enum, and the column carries a
     * CHECK constraint listing the names, so the two must agree.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(name = "vacation_type", nullable = false)
    private VacationType vacationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_band", nullable = false)
    private PriceBand priceBand;

    @Column(nullable = false)
    private String summary;

    protected DestinationEntity() {
        // Required by JPA.
    }

    /**
     * Convert to the domain type.
     *
     * The Destination constructor re-validates rather than trusting the row, so a record
     * that somehow bypassed the database constraints fails here instead of travelling on
     * into ranking and out to a client.
     */
    Destination toDomain() {
        return new Destination(slug, name, region, vacationType, priceBand, summary);
    }
}
