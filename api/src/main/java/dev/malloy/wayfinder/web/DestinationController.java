package dev.malloy.wayfinder.web;

import dev.malloy.wayfinder.domain.DestinationCatalog;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read access to the destination catalog.
 *
 * Milestone 1 deliberately stops here. The ranked top five of US-01 and the type filters
 * of US-04 are later milestones; shipping unranked endpoints first keeps the data layer
 * and the contract settled before any scoring is layered on top.
 */
@RestController
@RequestMapping(path = "/api/destinations", produces = MediaType.APPLICATION_JSON_VALUE)
class DestinationController {

    private final DestinationCatalog catalog;

    DestinationController(DestinationCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Every destination, ordered by slug.
     *
     * Unpaginated, which is a defensible choice at 30 rows and not at 30,000. The catalog
     * is reference data that grows by editorial decision rather than by user input, so the
     * size is known rather than hoped for. If it ever stops being known, this needs paging
     * before it needs anything else.
     */
    @GetMapping
    List<DestinationResponse> list() {
        return catalog.findAll().stream().map(DestinationResponse::from).toList();
    }

    @GetMapping("/{slug}")
    DestinationResponse bySlug(@PathVariable String slug) {
        return catalog.findBySlug(slug)
            .map(DestinationResponse::from)
            .orElseThrow(() -> new DestinationNotFoundException(slug));
    }
}
