package dev.malloy.wayfinder.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The domain invariants, tested without Spring or a database.
 *
 * That independence is the point of ADR-0001's layering. This class demonstrates it by
 * construction, but it does not enforce it: an earlier version of this comment claimed a
 * framework leaking into the domain would stop this test compiling, which was checked and
 * turned out to be false. ArchitectureTest is what actually enforces it.
 */
class DestinationTest {

    private static Destination valid(String summary) {
        return new Destination(
            "kyoto-mindfulness-retreat",
            "Kyoto Mindfulness Retreat",
            Region.ASIA,
            VacationType.WELLNESS,
            PriceBand.PREMIUM,
            summary
        );
    }

    @Nested
    @DisplayName("T-DOM-001: required fields must carry content (AC-01.3)")
    class RequiredFields {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void rejectsBlankSlug(String slug) {
            assertThatThrownBy(() -> new Destination(
                slug, "Name", Region.ASIA, VacationType.WELLNESS, PriceBand.PREMIUM, "A summary."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("slug");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void rejectsBlankName(String name) {
            assertThatThrownBy(() -> new Destination(
                "slug", name, Region.ASIA, VacationType.WELLNESS, PriceBand.PREMIUM, "A summary."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void rejectsBlankSummary(String summary) {
            assertThatThrownBy(() -> valid(summary))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("summary");
        }

        @Test
        void rejectsMissingRegion() {
            assertThatThrownBy(() -> new Destination(
                "slug", "Name", null, VacationType.WELLNESS, PriceBand.PREMIUM, "A summary."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("region");
        }

        @Test
        void rejectsMissingVacationType() {
            assertThatThrownBy(() -> new Destination(
                "slug", "Name", Region.ASIA, null, PriceBand.PREMIUM, "A summary."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacationType");
        }

        @Test
        void rejectsMissingPriceBand() {
            assertThatThrownBy(() -> new Destination(
                "slug", "Name", Region.ASIA, VacationType.WELLNESS, null, "A summary."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("priceBand");
        }

        @Test
        void stripsSurroundingWhitespaceRatherThanStoringIt() {
            Destination destination = new Destination(
                "slug", "  Kyoto  ", Region.ASIA, VacationType.WELLNESS, PriceBand.PREMIUM,
                "  A summary.  ");
            assertThat(destination.name()).isEqualTo("Kyoto");
            assertThat(destination.summary()).isEqualTo("A summary.");
        }
    }

    @Nested
    @DisplayName("T-DOM-002: summary length is bounded at 140 characters (AC-01.4)")
    class SummaryLength {

        @Test
        void acceptsExactlyTheLimit() {
            String atLimit = "x".repeat(Destination.SUMMARY_MAX_LENGTH);
            assertThatCode(() -> valid(atLimit)).doesNotThrowAnyException();
        }

        @Test
        void rejectsOneCharacterOverTheLimit() {
            String overLimit = "x".repeat(Destination.SUMMARY_MAX_LENGTH + 1);
            assertThatThrownBy(() -> valid(overLimit))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AC-01.4")
                .hasMessageContaining(String.valueOf(Destination.SUMMARY_MAX_LENGTH + 1));
        }

        @Test
        void acceptsASingleCharacter() {
            assertThatCode(() -> valid("x")).doesNotThrowAnyException();
        }

        @Test
        void measuresLengthAfterStrippingSoPaddingCannotSmuggleCharactersPastTheLimit() {
            String padded = "  " + "x".repeat(Destination.SUMMARY_MAX_LENGTH) + "  ";
            assertThatCode(() -> valid(padded)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Slug format")
    class SlugFormat {

        @ParameterizedTest
        @ValueSource(strings = {"kyoto", "kyoto-mindfulness-retreat", "a1", "x-1-y"})
        void acceptsLowercaseHyphenatedWords(String slug) {
            assertThatCode(() -> new Destination(
                slug, "Name", Region.ASIA, VacationType.WELLNESS, PriceBand.PREMIUM, "A summary."))
                .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Kyoto", "kyoto_retreat", "-kyoto", "kyoto-", "kyoto--retreat",
            "kyoto retreat", "kyoto/retreat"})
        void rejectsAnythingElse(String slug) {
            assertThatThrownBy(() -> new Destination(
                slug, "Name", Region.ASIA, VacationType.WELLNESS, PriceBand.PREMIUM, "A summary."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("slug");
        }
    }

    @Nested
    @DisplayName("PriceBand distance, which US-03 scoring will depend on")
    class PriceBandDistance {

        @Test
        void isZeroForTheSameBand() {
            assertThat(PriceBand.MODERATE.distanceTo(PriceBand.MODERATE)).isZero();
        }

        @Test
        void isSymmetric() {
            assertThat(PriceBand.BUDGET.distanceTo(PriceBand.LUXURY))
                .isEqualTo(PriceBand.LUXURY.distanceTo(PriceBand.BUDGET));
        }

        @Test
        void neverExceedsTheStatedMaximum() {
            for (PriceBand a : PriceBand.values()) {
                for (PriceBand b : PriceBand.values()) {
                    assertThat(a.distanceTo(b))
                        .isBetween(0, PriceBand.maximumDistance());
                }
            }
        }

        @Test
        void maximumDistanceIsActuallyReachable() {
            assertThat(PriceBand.BUDGET.distanceTo(PriceBand.LUXURY))
                .isEqualTo(PriceBand.maximumDistance());
        }
    }
}
