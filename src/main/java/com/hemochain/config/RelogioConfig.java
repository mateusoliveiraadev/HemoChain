package com.hemochain.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelogioConfig {
    @Bean public Clock relogio() { return Clock.systemUTC(); }
}
