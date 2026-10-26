package dev.malloy.wayfinder.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.malloy.wayfinder.domain.Destination;
import dev.malloy.wayfinder.domain.DestinationCatalog;
import dev.malloy.wayfinder.domain.PriceBand;
import dev.malloy.wayfinder.domain.Region;
import dev.malloy.wayfinder.domain.VacationType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The HTTP contract, with the catalog stubbed.
 *
 * No database here on purpose. What is under test is the shape of the response and the
 * behaviour on a missing slug, neither of which needs Postgres, and keeping it out means
 * this runs in a second rather than a minute.
 *
 * Carries T-API-001 through T-API-003.
 */
@WebMvcTest(DestinationController.class)
class DestinationControllerTest {

    private static final Destination KYOTO = new Destination(
        "kyoto-mindfulness-retreat",
        "Kyoto Mindfulness Retreat",
        Region.ASIA,
        VacationType.WELLNESS,
        PriceBand.PREMIUM,
        "Temple stays, morning zazen and tea ceremony in a quiet corner of the old capital."
    );

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DestinationCatalog catalog;

    @Test
    @DisplayName("T-API-001: the list response carries all four required fields (AC-01.3)")
    void listResponseCarriesTheRequiredFields() throws Exception {
        given(catalog.findAll()).willReturn(List.of(KYOTO));

        mvc.perform(get("/api/destinations"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].name").value("Kyoto Mindfulness Retreat"))
            .andExpect(jsonPath("$[0].vacationType").value("WELLNESS"))
            .andExpect(jsonPath("$[0].region").value("ASIA"))
            .andExpect(jsonPath("$[0].summary").isNotEmpty())
            .andExpect(jsonPath("$[0].slug").value("kyoto-mindfulness-retreat"));
    }

    @Test
    @DisplayName("An empty catalog is an empty array, not an error")
    void emptyCatalogIsAnEmptyArray() throws Exception {
        given(catalog.findAll()).willReturn(List.of());

        mvc.perform(get("/api/destinations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("T-API-002: a known slug returns its destination")
    void knownSlugReturnsTheDestination() throws Exception {
        given(catalog.findBySlug("kyoto-mindfulness-retreat")).willReturn(Optional.of(KYOTO));

        mvc.perform(get("/api/destinations/kyoto-mindfulness-retreat"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("kyoto-mindfulness-retreat"))
            .andExpect(jsonPath("$.priceBand").value("PREMIUM"));
    }

    @Test
    @DisplayName("T-API-003: an unknown slug is a 404 problem response naming the slug")
    void unknownSlugIsAProblemResponse() throws Exception {
        given(catalog.findBySlug(any())).willReturn(Optional.empty());

        mvc.perform(get("/api/destinations/no-such-destination"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Destination not found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.slug").value("no-such-destination"));
    }

    @Test
    @DisplayName("T-API-004: an error response leaks no stack trace or internal type names")
    void errorResponseLeaksNothingInternal() throws Exception {
        given(catalog.findBySlug(any())).willReturn(Optional.empty());

        String body = mvc.perform(get("/api/destinations/no-such-destination"))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
            .doesNotContain("dev.malloy")
            .doesNotContain("Exception")
            .doesNotContain("at java.")
            .doesNotContain("org.springframework");
    }
}
