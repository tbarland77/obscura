package io.github.tbarland.obscura.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry
        .addMapping("/api/**") // Allow CORS for all API endpoints
        .allowedOrigins(
            "http://localhost:5173", // Vite default dev server
            "http://localhost:3000", // React dev server (if using CRA)
            "http://127.0.0.1:5173" // Alternative localhost
            )
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600); // Cache preflight requests for 1 hour
  }
}
