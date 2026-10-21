package com.springai.jdd.conference.tools;

import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ConferenceToolsTest {

    private final ScheduleTools schedule = new ScheduleTools();

    @Test
    void exposesOverviewAndScheduleAsTools() {
        ToolCallback[] overview = ToolCallbacks.from(new OverviewTools());
        ToolCallback[] agenda = ToolCallbacks.from(schedule);

        assertThat(Arrays.stream(overview).map(tool -> tool.getToolDefinition().name()))
                .containsExactly("getConferenceOverview");
        assertThat(Arrays.stream(agenda).map(tool -> tool.getToolDefinition().name()))
                .containsExactly("getConferenceSchedule");
    }

    @Test
    void returnsTheAgendaAsPlainTextRatherThanAJsonString() {
        ToolCallback agenda = ToolCallbacks.from(schedule)[0];

        assertThat(agenda.call("{}")).startsWith(Agenda.HEADER).contains("\n── Day 1 · Tuesday 20 October ──");
    }

    @Test
    void overviewNamesDatesAndVenue() {
        assertThat(new OverviewTools().getConferenceOverview())
                .contains("20 October", "21 October 2026", "Hotel Metropolo", "Kraków");
    }

    @Test
    void filtersByDayAndRoom() {
        String track1 = schedule.getConferenceSchedule(2, "Track 1", null, null);

        assertThat(track1).contains("The Future of Software Architecture in the Age of AI",
                                    "Modular Monoliths with Spring Modulith")
                          .doesNotContain("Contract Testing", "Zero-Trust", "Event-Driven Architecture");
        assertThat(schedule.getConferenceSchedule(2, "1", null, null)).isEqualTo(track1);
    }

    @Test
    void matchesWholeWordsSoAiDoesNotMatchMaintain() {
        String aiTrack = schedule.getConferenceSchedule(null, null, "AI", null);

        assertThat(aiTrack).contains("Testing LLM-Powered Features", "RAG Beyond the Demo")
                           .doesNotContain("Valhalla", "Lunch");
    }

    @Test
    void findsTheSpeakersTalkByKeyword() {
        assertThat(schedule.getConferenceSchedule(null, null, null, "Bălașa"))
                .contains("15:00–15:45 | The Future of Software Architecture in the Age of AI — Track 1")
                .doesNotContain("Day 1");
    }

    @Test
    void saysExplicitlyWhenATalksSpeakersAreNotAnnounced() {
        assertThat(schedule.getConferenceSchedule(2, "Track 1", null, "Modulith"))
                .contains("Modular Monoliths with Spring Modulith — Track 1", "Speakers: to be announced");
    }

    @Test
    void saysSoWhenNothingMatches() {
        assertThat(schedule.getConferenceSchedule(1, "Room Z", null, null)).isEqualTo(ScheduleTools.NO_MATCH);
    }
}
