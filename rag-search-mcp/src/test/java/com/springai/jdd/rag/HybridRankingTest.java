package com.springai.jdd.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HybridRankingTest {

    private static final Passage WIFI = new Passage("wifi", "Wi-Fi", "faq.md", 0.58);
    private static final Passage CONTACT = new Passage("contact", "Contact", "faq.md", 0.60);
    private static final Passage SPEAKER = new Passage("speaker", "## Adrian Coman", "speakers.md", null);
    private static final Passage SPEAKER_SEMANTIC = new Passage("speaker", "## Adrian Coman", "speakers.md", 0.41);

    @Test
    void shouldLiftAPassageOnlyTheKeywordSearchFound() {
        List<Passage> fused = HybridRanking.fuse(List.of(CONTACT, WIFI), List.of(SPEAKER), 3);

        assertThat(fused).extracting(Passage::id).containsExactly("contact", "speaker", "wifi");
    }

    @Test
    void shouldRankWhatBothSearchesAgreeOnFirstAndKeepItsSimilarity() {
        List<Passage> fused = HybridRanking.fuse(List.of(CONTACT, WIFI, SPEAKER_SEMANTIC), List.of(SPEAKER), 2);

        assertThat(fused).extracting(Passage::id).containsExactly("speaker", "contact");
        assertThat(fused.getFirst().similarity()).isEqualTo(0.41);
    }
}
