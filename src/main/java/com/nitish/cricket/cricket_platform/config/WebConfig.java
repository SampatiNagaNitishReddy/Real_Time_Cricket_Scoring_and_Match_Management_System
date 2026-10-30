package com.nitish.cricket.cricket_platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173" , "https://real-time-cricket-scoring-system-fr.vercel.app")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS" ,"PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}