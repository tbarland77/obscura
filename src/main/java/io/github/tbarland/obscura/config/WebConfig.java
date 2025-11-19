package io.github.tbarland.obscura.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for CORS (Cross-Origin Resource Sharing).
 *
 * <p>Configures CORS to allow frontend applications to connect to the API. Allowed origins: Vite
 * dev server (localhost:5173), React dev server (localhost:3000), and alternative localhost
 * (127.0.0.1:5173). Preflight requests are cached for 1 hour.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins("http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
  }
}
