package com.example.demo.shortenurl.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration to register interceptors.
 * 
 * Registers the RateLimitInterceptor for specific paths only:
 * - POST /api/urls/shorten
 * - POST /api/urls/shorten/custom
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register rate limit interceptor only for shorten endpoints
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/urls/shorten")
            .addPathPatterns("/api/urls/shorten/custom");
    }
}
