package io.github.tbarland.obscura.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WebConfigTests {

  @Autowired private MockMvc mockMvc;

  @Test
  void testCorsAllowsViteOrigin() throws Exception {
    mockMvc
        .perform(
            options("/api/stories")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
        .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
  }

  @Test
  void testCorsAllowsReactOrigin() throws Exception {
    mockMvc
        .perform(
            options("/api/stories")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
        .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
  }

  @Test
  void testCorsAllows127001Origin() throws Exception {
    mockMvc
        .perform(
            options("/api/stories")
                .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5173"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  @Test
  void testCorsBlocksUnauthorizedOrigin() throws Exception {
    mockMvc
        .perform(
            options("/api/stories")
                .header(HttpHeaders.ORIGIN, "http://evil-site.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isForbidden());
  }

  @Test
  void testCorsAllowsAllStandardMethods() throws Exception {
    String[] methods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

    for (String method : methods) {
      mockMvc
          .perform(
              options("/api/stories")
                  .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                  .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method))
          .andExpect(status().isOk())
          .andExpect(
              header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }
  }

  @Test
  void testCorsOnlyAppliesToApiEndpoints() throws Exception {
    // CORS should work for /api/**
    mockMvc
        .perform(
            options("/api/stories")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

    // CORS should also work for nested API paths
    mockMvc
        .perform(
            options("/api/stories/1")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void testCorsAllowsAllHeaders() throws Exception {
    mockMvc
        .perform(
            options("/api/stories")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
        .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
  }

  @Test
  void testCorsMaxAge() throws Exception {
    mockMvc
        .perform(
            options("/api/stories")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
  }
}
