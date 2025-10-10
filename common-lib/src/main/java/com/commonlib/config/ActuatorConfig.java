package com.commonlib.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorConfig {

    @Bean
    public InfoContributor commonInfoContributor() {
        return (Info.Builder builder) -> builder.withDetail("maintainer", "Payment Services Team")
                .withDetail("contact", "support@paymentservices.com");
    }
}