package com.ros.ewallet.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.ros.ewallet.common.Constants.DATE_FORMAT;
import static com.ros.ewallet.common.Constants.DATE_TIME_FORMAT;

/**
 * Configuration file used to configure settings of the application.
 */
@Configuration
public class AppConfig {

    @Bean
    public JsonMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            builder.withConfigOverride(LocalDate.class, override ->
                    override.setFormat(JsonFormat.Value.forPattern(DATE_FORMAT)));
            builder.withConfigOverride(LocalDateTime.class, override ->
                    override.setFormat(JsonFormat.Value.forPattern(DATE_TIME_FORMAT)));
        };
    }
}
