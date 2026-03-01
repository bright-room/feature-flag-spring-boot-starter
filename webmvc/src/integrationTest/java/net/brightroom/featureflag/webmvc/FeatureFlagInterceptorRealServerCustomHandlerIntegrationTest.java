package net.brightroom.featureflag.webmvc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagDisableController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FeatureFlagInterceptorRealServerCustomHandlerIntegrationTest.TestConfig.class)
class FeatureFlagInterceptorRealServerCustomHandlerIntegrationTest {

  @Configuration
  @EnableAutoConfiguration
  @Import({
    FeatureFlagDisableController.class,
    FeatureFlagInterceptorRealServerCustomHandlerIntegrationTest.CustomExceptionHandler.class
  })
  static class TestConfig {}

  @ControllerAdvice
  @Order(0)
  static class CustomExceptionHandler {

    @ExceptionHandler(FeatureFlagAccessDeniedException.class)
    ResponseEntity<String> handle(FeatureFlagAccessDeniedException e) {
      return ResponseEntity.status(503).body("custom: " + e.featureName());
    }
  }

  @Value("${local.server.port}")
  int port;

  @Test
  void customHandlerTakesPriority_whenFeatureIsDisabled() throws Exception {
    HttpResponse<String> response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/test/disable"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());

    assertEquals(503, response.statusCode());
    assertEquals("custom: disable-class-level-feature", response.body());
  }
}
