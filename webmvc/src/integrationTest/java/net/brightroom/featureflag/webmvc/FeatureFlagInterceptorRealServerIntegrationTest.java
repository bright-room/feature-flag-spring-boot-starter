package net.brightroom.featureflag.webmvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagMethodLevelController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FeatureFlagInterceptorRealServerIntegrationTest.TestConfig.class)
class FeatureFlagInterceptorRealServerIntegrationTest {

  @Configuration
  @EnableAutoConfiguration
  @Import(FeatureFlagMethodLevelController.class)
  static class TestConfig {}

  @Value("${local.server.port}")
  int port;

  @Test
  void shouldAllowAccess_whenFeatureIsEnabled() throws Exception {
    HttpResponse<String> response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/experimental-stage-endpoint"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    assertEquals("Allowed", response.body());
  }

  @Test
  void shouldBlockAccess_whenFeatureIsDisabled() throws Exception {
    HttpResponse<String> response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/development-stage-endpoint"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());

    assertEquals(403, response.statusCode());
    assertTrue(response.body().contains("Feature 'development-stage-endpoint' is not available"));
    assertTrue(response.body().contains("Feature flag access denied"));
  }
}
