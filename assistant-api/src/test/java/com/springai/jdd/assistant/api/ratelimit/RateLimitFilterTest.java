package com.springai.jdd.assistant.api.ratelimit;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

class RateLimitFilterTest {

    private static final String NOW = "2026-08-13T10:15:00Z";
    private static final String QUERY_PATH = "/query";
    private static final String HEALTH_PATH = "/actuator/health";
    private static final String CLIENT = "10.0.0.1";
    private static final String TOO_MANY_QUERIES = "Too many queries";
    private static final int BURST = 3;
    private static final int PER_SECOND = 1;
    private static final int MAX_CLIENTS = 2;
    private static final int BEYOND_THE_BURST = BURST + 1;

    private final FilterChain chain = mock(FilterChain.class);
    private final RateLimitFilter filter = new RateLimitFilter(Clock.fixed(Instant.parse(NOW), UTC),
                                                               new ObjectMapper(),
                                                               properties());

    private static RateLimitProperties properties() {
        return RateLimitProperties.builder()
                                  .burst(BURST)
                                  .perSecond(PER_SECOND)
                                  .maxClients(MAX_CLIENTS)
                                  .build();
    }

    @Test
    void shouldAnswerTheQueryItRejectsWithTheResponseContract() throws Exception {
        for (int query = 0; query < BURST; query++) {
            filter.doFilter(requestTo(QUERY_PATH), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(requestTo(QUERY_PATH), response, chain);

        assertThat(response.getStatus()).isEqualTo(TOO_MANY_REQUESTS.value());
        assertThat(response.getContentAsString()).contains(TOO_MANY_QUERIES);
        verify(chain, times(BURST)).doFilter(any(), any());
    }

    @Test
    void shouldLeaveEveryOtherEndpointAlone() throws Exception {
        for (int call = 0; call < BEYOND_THE_BURST; call++) {
            filter.doFilter(requestTo(HEALTH_PATH), new MockHttpServletResponse(), chain);
        }

        verify(chain, times(BEYOND_THE_BURST)).doFilter(any(), any());
    }

    @Test
    void shouldGiveEachClientBehindTheProxyItsOwnBucket() throws Exception {
        for (int query = 0; query < BURST; query++) {
            filter.doFilter(forwardedFor("203.0.113.7"), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse otherClient = new MockHttpServletResponse();
        filter.doFilter(forwardedFor("203.0.113.8, 10.0.0.1"), otherClient, chain);

        assertThat(otherClient.getStatus()).isNotEqualTo(TOO_MANY_REQUESTS.value());
        verify(chain, times(BEYOND_THE_BURST)).doFilter(any(), any());
    }

    private MockHttpServletRequest forwardedFor(String header) {
        MockHttpServletRequest request = requestTo(QUERY_PATH);
        request.addHeader("X-Forwarded-For", header);
        return request;
    }

    private MockHttpServletRequest requestTo(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        request.setRemoteAddr(CLIENT);
        return request;
    }
}
