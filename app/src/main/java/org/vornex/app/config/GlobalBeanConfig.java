package org.vornex.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class GlobalBeanConfig {
    @Bean
    Clock clock() { return Clock.systemUTC(); }
}

