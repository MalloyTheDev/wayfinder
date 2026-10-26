package dev.malloy.wayfinder.web;

/**
 * Raised when a slug matches no destination.
 *
 * Carries the slug so the handler can name it in the response, which is the difference
 * between a client knowing it asked for the wrong thing and a client guessing.
 */
class DestinationNotFoundException extends RuntimeException {

    private final String slug;

    DestinationNotFoundException(String slug) {
        super("No destination with slug: " + slug);
        this.slug = slug;
    }

    String slug() {
        return slug;
    }
}
