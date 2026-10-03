package com.springai.jdd.conference.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * An MCP tool is a plain Spring bean method: the annotations are the contract the model reads.
 * The filters exist for the model's sake — a small model reading a 35-session agenda mixes up rooms
 * and days; asking for "day 2, Room B" hands it only the rows it needs.
 */
@Component
public class ScheduleTools {

    static final String NO_MATCH = "No session matches these filters. Call getConferenceSchedule with fewer filters.";

    @Tool(name = "getConferenceSchedule",
          resultConverter = PlainTextResultConverter.class,
          description = """
                  Get sessions from the JDD 2026 agenda (20–21 October, Kraków): time, room, track, title, speakers \
                  and description, plus breaks, lunch and the after-party. All filters are optional and combine. \
                  Set only the filters the question states — never guess a day or room; to find a talk by topic, \
                  use the keyword alone. With no filters, returns the whole agenda.""")
    public String getConferenceSchedule(
            @ToolParam(description = "Only if the question names the day: 1 = Tuesday 20 October, 2 = Wednesday 21 October",
                       required = false) Integer day,
            @ToolParam(description = "Room name, e.g. \"Room B\"", required = false) String room,
            @ToolParam(description = "Words from a track name, e.g. \"AI\", \"Security\"", required = false) String track,
            @ToolParam(description = "Words in a title, description or speaker name, e.g. \"Spring AI\"",
                       required = false) String keyword) {
        List<Session> sessions = Agenda.SESSIONS.stream()
                                                .filter(session -> session.matches(day, room, track, keyword))
                                                .toList();
        return sessions.isEmpty() ? NO_MATCH : Agenda.format(sessions);
    }
}
