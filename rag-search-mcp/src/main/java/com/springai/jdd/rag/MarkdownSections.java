package com.springai.jdd.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a Markdown page into one chunk per {@code ##} section, each prefixed with the page title.
 * A chunk that covers exactly one topic ("Wi-Fi", "Getting there") embeds far more precisely than a
 * fixed-size window that mixes several, and the title keeps the chunk meaningful on its own.
 */
final class MarkdownSections {

    private static final String SECTION = "## ";
    private static final String TITLE = "# ";

    private MarkdownSections() {
    }

    static List<String> split(String markdown) {
        String title = "";
        List<String> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : markdown.split("\\R")) {
            if (line.startsWith(TITLE)) {
                title = line.substring(TITLE.length()).trim();
                continue;
            }
            if (line.startsWith(SECTION)) {
                flush(title, current, sections);
                current = new StringBuilder();
            }
            current.append(line).append('\n');
        }
        flush(title, current, sections);
        return sections;
    }

    private static void flush(String title, StringBuilder section, List<String> sections) {
        String body = section.toString().strip();
        if (!body.isEmpty()) {
            sections.add(title.isEmpty() ? body : title + "\n" + body);
        }
    }
}
