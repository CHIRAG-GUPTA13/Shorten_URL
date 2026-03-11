package com.example.demo.shortenurl.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson ObjectMapper configuration for handling Java 8 date/time types
 * with flexible date format support.
 */
@Configuration
public class JacksonConfig {

    /**
     * Flexible date time formatter that accepts multiple formats:
     * - yyyy-MM-dd'T'HH:mm:ss (full ISO with seconds)
     * - yyyy-MM-dd'T'HH:mm (ISO without seconds)
     * - yyyy-MM-dd HH:mm:ss (space separator with seconds)
     * - yyyy-MM-dd (date only)
     */
    private static final DateTimeFormatter FLEXIBLE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
            "[yyyy-MM-dd'T'HH:mm:ss][yyyy-MM-dd'T'HH:mm][yyyy-MM-dd HH:mm:ss][yyyy-MM-dd]");

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTimeModule for Java 8 date/time support
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // Configure flexible deserializer for LocalDateTime
        LocalDateTimeDeserializer deserializer = new LocalDateTimeDeserializer(FLEXIBLE_DATE_TIME_FORMATTER);
        javaTimeModule.addDeserializer(LocalDateTime.class, deserializer);

        // Configure serializer for LocalDateTime
        LocalDateTimeSerializer serializer = new LocalDateTimeSerializer(FLEXIBLE_DATE_TIME_FORMATTER);
        javaTimeModule.addSerializer(LocalDateTime.class, serializer);

        mapper.registerModule(javaTimeModule);

        // Configure serialization features
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }
}
