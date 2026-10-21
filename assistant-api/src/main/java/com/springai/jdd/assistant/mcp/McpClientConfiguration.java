package com.springai.jdd.assistant.mcp;

import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
class McpClientConfiguration {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);

    @Bean
    McpClientCustomizer<HttpClientStreamableHttpTransport.Builder> failFastMcpConnections() {
        return (connectionName, transport) -> transport.connectTimeout(CONNECT_TIMEOUT);
    }
}
