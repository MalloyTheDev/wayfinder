package dev.malloy.wayfinder.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns exceptions into RFC 9457 problem responses.
 *
 * Errors are a published part of the contract, not an afterthought, so they get a defined
 * shape rather than whatever the container would have produced. The detail names what was
 * asked for and what is wrong with it, and nothing else: no stack trace, no SQL, no class
 * names, since those tell an attacker more than they tell a client.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(DestinationNotFoundException.class)
    ProblemDetail handleNotFound(DestinationNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Destination not found");
        problem.setDetail("No destination has the slug '" + exception.slug() + "'.");
        problem.setProperty("slug", exception.slug());
        return problem;
    }
}
