package com.reto.tecnico.account_service.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    @Bean
    public Clock systemUTC() {
        return Clock.systemUTC();
    }
}
