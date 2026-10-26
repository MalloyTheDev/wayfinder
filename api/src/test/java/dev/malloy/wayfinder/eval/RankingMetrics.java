package dev.malloy.wayfinder.eval;

import java.util.List;
import java.util.Map;

/**
 * Normalised Discounted Cumulative Gain.
 *
 * The standard measure of ranking quality when relevance is graded rather than binary. It
 * rewards putting highly relevant results near the top and discounts gains logarithmically
 * by position, so moving an ideal result from rank 8 to rank 1 matters more than moving a
 * marginal one.
 *
 * Why this and not "the top result is correct": a ranking metric is a number that can
 * genuinely get worse when the code changes. An assertion that an endpoint returns HTTP
 * 200 cannot. That difference is the reason this harness exists.
 *
 * Gain is exponential, 2^rel - 1, which is the formulation that makes a grade-3 result
 * worth meaningfully more than two grade-1 results rather than merely slightly more.
 */
final class RankingMetrics {

    private RankingMetrics() {
    }

    /**
     * NDCG at cut-off k for one ranking.
     *
     * @param rankedSlugs the ranking under test, best first
     * @param relevance   graded judgments; an absent slug counts as 0
     * @param k           cut-off position
     * @return 0.0 to 1.0, where 1.0 means the ranking matched the ideal ordering
     */
    static double ndcgAt(List<String> rankedSlugs, Map<String, Integer> relevance, int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be positive, got " + k);
        }

        double dcg = discountedGain(
            rankedSlugs.stream().limit(k).map(slug -> relevance.getOrDefault(slug, 0)).toList());

        // The best achievable ordering: every judged result, most relevant first.
        double idealDcg = discountedGain(
            relevance.values().stream().sorted((a, b) -> Integer.compare(b, a)).limit(k).toList());

        // No relevant results judged at all. Reporting 0.0 would read as a failing ranking
        // when in fact the question is unanswerable, so this is caller error.
        if (idealDcg == 0.0) {
            throw new IllegalStateException(
                "no judged relevance above zero, so NDCG is undefined for this persona");
        }

        return dcg / idealDcg;
    }

    private static double discountedGain(List<Integer> grades) {
        double sum = 0.0;
        for (int position = 0; position < grades.size(); position++) {
            double gain = Math.pow(2, grades.get(position)) - 1;
            // log2(position + 2), because positions are zero-based here and the standard
            // discount for rank 1 is log2(2) = 1.
            double discount = Math.log(position + 2.0) / Math.log(2.0);
            sum += gain / discount;
        }
        return sum;
    }
}
