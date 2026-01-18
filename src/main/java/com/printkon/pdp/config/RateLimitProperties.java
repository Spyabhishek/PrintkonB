package com.printkon.pdp.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.rate-limiting")
public class RateLimitProperties {
    
    private boolean enabled = true;
    private int productReadLimit = 100;
    private int searchLimit = 30;
    private int generalLimit = 200;
    private Duration window = Duration.ofMinutes(1);
}