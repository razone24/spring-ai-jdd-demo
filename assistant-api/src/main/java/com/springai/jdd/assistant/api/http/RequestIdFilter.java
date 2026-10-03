package com.springai.jdd.assistant.api.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import static com.springai.jdd.assistant.audit.RequestId.KEY;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.util.StringUtils.hasText;

@Component
@Order(HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put(KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String provided = request.getHeader(REQUEST_ID_HEADER);
        return hasText(provided) ? provided : UUID.randomUUID().toString();
    }
}
