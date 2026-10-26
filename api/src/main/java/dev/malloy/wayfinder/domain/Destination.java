package dev.malloy.wayfinder.domain;

import java.util.regex.Pattern;

/**
 * A place a traveller can be shown.
 *
 * This type is deliberately free of Spring, JPA and Jackson. ADR-0001 commits to keeping
 * the domain independent of the framework and the database so the ordering invariants in
 * US-03 can be tested without starting either.
 *
 * The constructor enforces two acceptance criteria rather than trusting callers:
 * AC-01.3, that the fields a traveller needs in order to choose are present and non-blank,
 * and AC-01.4, that a summary cannot run long enough to break a layout. Enforcing them
 * here means an invalid Destination cannot exist, whatever route the data arrived by.
 *
 * @param slug          stable URL-safe identifier, lowercase words separated by hyphens
 * @param name          display name
 * @param region        broad geographic area, used for filtering and scoring
 * @param vacationType  the kind of trip this is, which US-04 filters on
 * @param priceBand     affordability bracket, used for scoring in US-03
 * @param summary       one line a traveller reads before deciding to look closer
 */
public record Destination(
    String slug,
    String name,
    Region region,
    VacationType vacationType,
    PriceBand priceBand,
    String summary
) {

    /** AC-01.4. The database carries the same bound as a CHECK constraint. */
    public static final int SUMMARY_MAX_LENGTH = 140;

    /** Lowercase alphanumeric words joined by single hyphens, no leading or trailing hyphen. */
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    public Destination {
        slug = requireText(slug, "slug");
        name = requireText(name, "name");
        summary = requireText(summary, "summary");
        requireNotNull(region, "region");
        requireNotNull(vacationType, "vacationType");
        requireNotNull(priceBand, "priceBand");

        if (!SLUG.matcher(slug).matches()) {
            throw new IllegalArgumentException(
                "slug must be lowercase words separated by single hyphens, got: " + slug);
        }
        if (summary.length() > SUMMARY_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "summary must be at most " + SUMMARY_MAX_LENGTH + " characters (AC-01.4), got "
                    + summary.length() + " for slug: " + slug);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must be present and non-blank (AC-01.3)");
        }
        return value.strip();
    }

    private static void requireNotNull(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must be present (AC-01.3)");
        }
    }
}
