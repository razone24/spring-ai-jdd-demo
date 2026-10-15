package com.springai.jdd.conference.tools;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * One agenda slot. {@code day} is 1 (Tuesday 20 October) or 2 (Wednesday 21 October).
 */
public record Session(int day,
                      String start,
                      String end,
                      String room,
                      String track,
                      String title,
                      String speakers,
                      String description) {

    // Said explicitly: a blank field invites a small model to borrow the speakers of the row above.
    static final String SPEAKERS_TBA = "to be announced";

    /** Every given filter must match; a missing filter matches everything. */
    boolean matches(Integer day, String room, String track, String keyword) {
        return (day == null || this.day == day)
               && containsWords(this.room, room)
               && containsWords(this.track, track)
               && containsWords(String.join(" ", title, orEmpty(speakers), orEmpty(description), orEmpty(this.track)),
                                keyword);
    }

    String format() {
        StringBuilder line = new StringBuilder()
                .append(start).append('–').append(end).append(" | ").append(title).append(" — ").append(room);
        if (track != null) {
            line.append("\n  Track: ").append(track)
                .append("\n  Speakers: ").append(speakers == null ? SPEAKERS_TBA : speakers);
        }
        if (description != null) {
            line.append("\n  Description: ").append(description);
        }
        return line.toString();
    }

    /**
     * Every word of the filter appears in the text as a whole word — "AI" matches "Age of AI", not
     * "maintain"; "Room B" and "B" both match room "Room B".
     */
    private static boolean containsWords(String text, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        if (text == null) {
            return false;
        }
        String haystack = text.toLowerCase(Locale.ROOT);
        return Arrays.stream(filter.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                     .filter(word -> !word.isEmpty() && !word.equals("room"))
                     .allMatch(word -> Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(word) + "(?![\\p{L}\\p{N}])")
                                              .matcher(haystack)
                                              .find());
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
