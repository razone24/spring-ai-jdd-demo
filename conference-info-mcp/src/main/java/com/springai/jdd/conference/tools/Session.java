package com.springai.jdd.conference.tools;

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

    static final String[] DAY_NAMES = {"", "Day 1 · Tuesday 20 October", "Day 2 · Wednesday 21 October"};
    // Said explicitly: a blank field invites a small model to borrow the speakers of the row above.
    static final String SPEAKERS_TBA = "to be announced";

    String format() {
        StringBuilder line = new StringBuilder()
                .append(start).append('–').append(end).append(" | ").append(title).append(" — ").append(room);
        if (track != null) {
            line.append("\n  Track: ").append(track);
        }
        if (track != null) {
            line.append("\n  Speakers: ").append(speakers == null ? SPEAKERS_TBA : speakers);
        }
        if (description != null) {
            line.append("\n  Description: ").append(description);
        }
        return line.toString();
    }
}
