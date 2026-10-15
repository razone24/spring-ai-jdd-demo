package com.springai.jdd.assistant.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CoreConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
