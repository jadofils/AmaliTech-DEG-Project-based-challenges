// config/TimerConfig.java
package com.watchdog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
public class TimerConfig {
    // Configuration is handled by @EnableAsync and @EnableScheduling
}