package com.springai.jdd.rag;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal rank fusion: every list votes {@code 1 / (K + rank)} for each passage it returned, and
 * passages are ordered by their total. It needs no score calibration between the two searches, and a
 * passage both searches agree on rises to the top.
 */
final class HybridRanking {

    private static final int K = 60;

    private HybridRanking() {
    }

    static List<Passage> fuse(List<Passage> semantic, List<Passage> keyword, int limit) {
        Map<String, Double> votes = new LinkedHashMap<>();
        Map<String, Passage> passages = new LinkedHashMap<>();
        vote(semantic, votes, passages);
        vote(keyword, votes, passages);
        return votes.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                    .limit(limit)
                    .map(entry -> passages.get(entry.getKey()))
                    .toList();
    }

    private static void vote(List<Passage> ranked, Map<String, Double> votes, Map<String, Passage> passages) {
        for (int rank = 0; rank < ranked.size(); rank++) {
            Passage passage = ranked.get(rank);
            votes.merge(passage.id(), 1.0 / (K + rank + 1), Double::sum);
            // Keep the vector search's copy: it carries the similarity score.
            passages.merge(passage.id(), passage, (kept, other) -> kept.similarity() != null ? kept : other);
        }
    }
}
