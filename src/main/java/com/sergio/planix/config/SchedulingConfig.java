package com.sergio.planix.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "planix.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
