package com.springai.jdd.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownSectionsTest {

    @Test
    void shouldSplitOnSectionsAndKeepThePageTitleOnEachChunk() {
        String page = """
                # Venue FAQ

                Intro line.

                ## Wi-Fi

                Network JDD2026.

                ## Food

                Lunch at 12:00.
                """;

        assertThat(MarkdownSections.split(page)).containsExactly("Venue FAQ\nIntro line.",
                                                                 "Venue FAQ\n## Wi-Fi\n\nNetwork JDD2026.",
                                                                 "Venue FAQ\n## Food\n\nLunch at 12:00.");
    }
}
