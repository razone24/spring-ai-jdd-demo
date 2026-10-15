package com.springai.jdd.assistant.api.ratelimit;

import com.springai.jdd.assistant.api.QueryResponseFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;

import static com.springai.jdd.assistant.api.ApiPaths.QUERY;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.util.StringUtils.hasText;

@Component
@Order(HIGHEST_PRECEDENCE + 1)
@EnableConfigurationProperties(RateLimitProperties.class)
class RateLimitFilter extends OncePerRequestFilter {

    private static final String TOO_MANY_QUERIES_MESSAGE = "Too many queries from this client. Please retry shortly.";
    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final ClientBuckets buckets;
    private final ObjectMapper objectMapper;

    RateLimitFilter(Clock clock, ObjectMapper objectMapper, RateLimitProperties properties) {
        this.objectMapper = objectMapper;
        this.buckets = new ClientBuckets(clock, properties);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !QUERY.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (buckets.tryConsumeFor(clientOf(request))) {
            chain.doFilter(request, response);
            return;
        }
        reject(response);
    }

    /**
     * Behind the web app's reverse proxy every request comes from the proxy's address, so the
     * client is the first hop the proxy reports. Only trust this header behind your own proxy.
     */
    private String clientOf(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR);
        if (hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(TOO_MANY_REQUESTS.value());
        response.setContentType(APPLICATION_JSON_VALUE);
        response.getWriter()
                .write(objectMapper.writeValueAsString(QueryResponseFactory.buildError(TOO_MANY_QUERIES_MESSAGE)));
    }
}
