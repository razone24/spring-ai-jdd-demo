package com.springai.jdd.conference.tools;

import java.util.List;

/**
 * Demo agenda. The official JDD 2026 schedule was not yet published when this demo was built, so
 * apart from the conference frame (dates, tracks, community programme) these slots are sample data.
 */
final class Agenda {

    private static final String AI = "AI-Augmented Engineering";
    private static final String JVM = "Backend & Java/JVM";
    private static final String ARCHITECTURE = "Architecture & Distributed Systems";
    private static final String CLOUD = "Cloud, DevOps & Platform Engineering";
    private static final String QUALITY = "Software Quality, Testing & Reliability";
    private static final String SECURITY = "Security";
    private static final String FUTURE = "Future Software Engineering";
    private static final String LEADERSHIP = "Engineering Leadership & Soft Skills";

    static final List<Session> SESSIONS = List.of(
            new Session(1, "08:00", "09:00", "Lobby", null, "Registration & badge pick-up, coffee", null, null),
            new Session(1, "09:00", "09:15", "Room A", null, "Opening — 20 years of JDD", null, null),
            new Session(1, "09:15", "10:00", "Room A", FUTURE, "KEYNOTE — The Next Decade of Software Engineering", null,
                        "Where the craft is heading when code, reviews and operations are shared with AI."),
            new Session(1, "10:00", "10:20", "Foyer", null, "Coffee break", null, null),
            new Session(1, "10:20", "11:05", "Room A", JVM, "Java 26 and Beyond: What Valhalla Changes for Your Code", null,
                        "Value classes, null-restricted types and what they mean for everyday collections and DTOs."),
            new Session(1, "10:20", "11:05", "Room B", ARCHITECTURE, "Event-Driven Architecture Without the Regrets", null,
                        "Outbox, idempotency and replay — the patterns that keep event-driven systems debuggable."),
            new Session(1, "10:20", "11:05", "Room C", CLOUD, "Platform Engineering: Golden Paths That Developers Actually Use",
                        null, null),
            new Session(1, "11:15", "12:00", "Room A", JVM, "Virtual Threads in Production: Two Years Later", null,
                        "Pinning, connection pools and observability lessons from real workloads."),
            new Session(1, "11:15", "12:00", "Room D", AI, "Testing LLM-Powered Features: From Vibes to Evals", null,
                        "Golden datasets, LLM-as-judge and regression suites for non-deterministic code."),
            new Session(1, "12:00", "13:00", "Restaurant, ground floor", null, "Lunch", null, null),
            new Session(1, "13:00", "13:45", "Room A", JVM, "Spring Boot 4 and Spring AI 2 in Practice", null, null),
            new Session(1, "13:00", "13:45", "Room C", SECURITY, "Supply-Chain Security for JVM Projects", null, null),
            new Session(1, "13:00", "17:00", "JUG Lounge", null, "JUGmajster community sessions", null,
                        "Java User Groups from Kraków, Warsaw, Poznań, Wrocław, Tricity and visiting CEE groups."),
            new Session(1, "14:00", "14:45", "Room B", JVM, "Kotlin Coroutines vs Java Structured Concurrency", null, null),
            new Session(1, "14:00", "14:45", "Room D", SECURITY + " / " + AI, "Prompt Injection Is the New SQL Injection",
                        null, null),
            new Session(1, "15:00", "15:45", "Room D", AI, "Observability for AI Agents: Tokens, Traces and Cost", null, null),
            new Session(1, "15:45", "16:05", "Foyer", null, "Coffee break", null, null),
            new Session(1, "16:05", "16:50", "Room B", LEADERSHIP, "Leading Engineering Teams Through the AI Transition",
                        null, null),
            new Session(1, "17:00", "18:00", "Rooms B–D", null, "Unconference — open space, topics proposed on the day",
                        null, null),
            new Session(1, "19:30", "late", "Hotel Metropolo rooftop & bar", null, "JDD After-Party", null,
                        "Badge required; drinks and snacks included."),
            new Session(2, "08:30", "09:00", "Lobby", null, "Coffee", null, null),
            new Session(2, "09:00", "09:45", "Room A", ARCHITECTURE, "KEYNOTE — Architecture Is Still About Trade-offs",
                        null, null),
            new Session(2, "10:00", "10:45", "Room A", JVM, "GraalVM Native Images for Spring Services", null, null),
            new Session(2, "10:00", "10:45", "Room C", CLOUD, "Kubernetes Cost Optimisation for Java Workloads", null, null),
            new Session(2, "11:00", "11:45", "Room B", ARCHITECTURE + " / " + AI,
                        "The Future of Software Architecture in the Age of AI",
                        "Răzvan Bălașa (Playtika) & Adrian Coman (CrowdStrike)",
                        "From a naive \"whole wiki in the prompt\" chatbot to RAG, a semantic cache and an agentic MCP "
                        + "orchestrator — with a live Spring AI demo on a local LLM, guardrails, memory, audit and "
                        + "Grafana dashboards."),
            new Session(2, "11:00", "11:45", "Room C", QUALITY, "Contract Testing in a Microservice Landscape", null, null),
            new Session(2, "12:00", "13:00", "Restaurant, ground floor", null, "Lunch", null, null),
            new Session(2, "13:00", "13:45", "Room D", AI, "Building MCP Servers in Java", null,
                        "Exposing your services as tools for AI agents with the Model Context Protocol."),
            new Session(2, "13:00", "13:45", "Room C", QUALITY, "Chaos Engineering on a Budget", null, null),
            new Session(2, "14:00", "14:45", "Room D", AI, "RAG Beyond the Demo: Chunking, Re-ranking and Freshness", null,
                        null),
            new Session(2, "14:00", "14:45", "Room B", ARCHITECTURE, "Modular Monoliths with Spring Modulith", null, null),
            new Session(2, "15:00", "15:45", "Room C", SECURITY, "Zero-Trust for Internal APIs", null, null),
            new Session(2, "15:45", "16:05", "Foyer", null, "Coffee break", null, null),
            new Session(2, "16:05", "16:50", "Room A", FUTURE, "CLOSING KEYNOTE — Engineers, Agents and the Craft", null,
                        null),
            new Session(2, "16:50", "17:00", "Room A", null, "Closing & raffle", null, null));

    static final String HEADER = "JDD 2026 AGENDA (demo data) · Hotel Metropolo, Kraków";
    private static final String[] DAY_NAMES = {"", "Day 1 · Tuesday 20 October", "Day 2 · Wednesday 21 October"};

    private Agenda() {
    }

    /** The sessions as text the model reads well: grouped under a heading per day. */
    static String format(List<Session> sessions) {
        StringBuilder agenda = new StringBuilder(HEADER);
        int currentDay = 0;
        for (Session session : sessions) {
            if (session.day() != currentDay) {
                currentDay = session.day();
                agenda.append("\n\n── ").append(DAY_NAMES[currentDay]).append(" ──");
            }
            agenda.append('\n').append(session.format());
        }
        return agenda.toString();
    }
}
