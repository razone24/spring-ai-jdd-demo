package com.springai.jdd.conference.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class OverviewTools {

    @Tool(name = "getConferenceOverview",
          resultConverter = PlainTextResultConverter.class,
          description = """
                  Get the official overview of JDD 2026 (Java Developers Day), Kraków, 20–21 October 2026. \
                  Returns what the conference is, dates, venue and address, tracks, audience size, \
                  ticket prices and what a ticket includes, community programme and organiser. \
                  Use this for general questions about the event itself: what, when, where, how much, who organises.""")
    public String getConferenceOverview() {
        return """
                JDD 2026 — JAVA DEVELOPERS DAY
                ==============================
                Tagline: The Future of Software Engineering Starts Here
                Edition: 20 years of JDD community history

                WHEN
                  Tuesday 20 October – Wednesday 21 October 2026
                  Doors open 08:00, first session 09:00 (CEST)

                WHERE
                  Hotel Metropolo, ul. Orzechowa 11, Kraków, Poland

                SCALE
                  ~600 engineers · 50+ speakers · 60+ sessions over 2 days · 30+ technology partners

                TRACKS (8)
                  1. Backend & Java/JVM (Java, JVM, Kotlin, Scala)
                  2. Architecture & Distributed Systems
                  3. Cloud, DevOps & Platform Engineering
                  4. Software Quality, Testing & Reliability
                  5. Security
                  6. Platform
                  7. AI-Augmented Engineering (AI for developers)
                  8. Future Software Engineering & Engineering Leadership

                TICKETS (PLN, net)
                  Standard (II round, until 16 Oct 2026): 1 399
                  TeamPass (5 tickets):                    5 995
                  Student (under 26, valid student ID):     299
                  Add-ons: T-shirt +50, Hoodie +100, both +150
                  A ticket includes: both days, all tracks, JUGmajster, unconference sessions,
                  meals, the after-party and access to the session recordings.
                  Tickets are sold on Eventory (eventory.cc/event/jdd-2026/tickets).

                COMMUNITY
                  JUGmajster — Java User Groups from Kraków, Warsaw, Poznań, Wrocław, Tricity and
                  visiting CEE groups run community sessions alongside the main tracks.
                  Unconference — open-space sessions proposed by attendees on the day.

                ORGANISER
                  PROIDEA — 20+ years of tech conferences and hackathons in Poland and CEE.
                  Website: jdd.org.pl · Social: @JDD_Krakow (X), JDDconf (Facebook, LinkedIn, YouTube)
                """;
    }
}
