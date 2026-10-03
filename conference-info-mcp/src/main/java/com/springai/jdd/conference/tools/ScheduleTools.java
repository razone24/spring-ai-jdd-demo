package com.springai.jdd.conference.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * The agenda as a filterable tool. A small model reading a 35-session agenda mixes up rooms and
 * days; letting it ask for "day 2, Room B" hands it only the rows it needs.
 */
@Component
public class ScheduleTools {

    static final String HEADER = "JDD 2026 AGENDA (demo data) · Hotel Metropolo, Kraków";
    static final String NO_MATCH = "No session matches these filters. Call getConferenceSchedule with fewer filters.";
    private static final String ROOM_WORD = "room";

    @Tool(name = "getConferenceSchedule",
          resultConverter = PlainTextResultConverter.class,
          description = """
                  Get sessions from the JDD 2026 agenda (20–21 October, Kraków): time, room, track, title, speakers \
                  and description, plus breaks, lunch and the after-party. All filters are optional and combine. \
                  Set only the filters the question states — never guess a day or room; to find a talk by topic, \
                  use the keyword alone. With no filters, returns the whole agenda.""")
    public String getConferenceSchedule(
            @ToolParam(description = "Only if the question names the day: 1 = Tuesday 20 October, 2 = Wednesday 21 October",
                       required = false)
            Integer day,
            @ToolParam(description = "Room name, e.g. \"Room B\"", required = false)
            String room,
            @ToolParam(description = "Words from a track name, e.g. \"AI\", \"Security\", \"Architecture\"", required = false)
            String track,
            @ToolParam(description = "Words to find in a title, description or speaker name, e.g. \"Spring AI\"",
                       required = false)
            String keyword) {
        Predicate<Session> filter = session -> (day == null || session.day() == day)
                                               && matches(session.room(), room)
                                               && matches(session.track(), track)
                                               && matches(searchableText(session), keyword);
        List<Session> sessions = Agenda.SESSIONS.stream().filter(filter).toList();
        return sessions.isEmpty() ? NO_MATCH : format(sessions);
    }

    private static String format(List<Session> sessions) {
        StringBuilder agenda = new StringBuilder(HEADER);
        int currentDay = 0;
        for (Session session : sessions) {
            if (session.day() != currentDay) {
                currentDay = session.day();
                agenda.append("\n\n── ").append(Session.DAY_NAMES[currentDay]).append(" ──");
            }
            agenda.append('\n').append(session.format());
        }
        return agenda.toString();
    }

    private static String searchableText(Session session) {
        return String.join(" ", session.title(), nullToEmpty(session.speakers()), nullToEmpty(session.description()),
                           nullToEmpty(session.track()));
    }

    /**
     * Every term of the filter must appear in the text as a whole word ("AI" matches "Age of AI",
     * not "maintain"); a blank filter matches everything.
     */
    private static boolean matches(String text, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        if (text == null) {
            return false;
        }
        List<String> terms = Arrays.stream(filter.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                                   .filter(term -> !term.isEmpty() && !term.equals(ROOM_WORD))
                                   .toList();
        String haystack = text.toLowerCase(Locale.ROOT);
        return terms.stream().allMatch(term -> Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(term)
                                                               + "(?![\\p{L}\\p{N}])").matcher(haystack).find());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
