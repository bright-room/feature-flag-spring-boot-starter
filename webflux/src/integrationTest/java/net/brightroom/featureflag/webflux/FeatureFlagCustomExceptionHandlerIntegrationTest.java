package net.brightroom.featureflag.webflux;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxTestAutoConfiguration;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagDisableController;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagMethodLevelController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@WebFluxTest(
    controllers = {FeatureFlagDisableController.class, FeatureFlagMethodLevelController.class})
@Import({
  FeatureFlagWebFluxTestAutoConfiguration.class,
  FeatureFlagCustomExceptionHandlerIntegrationTest.CustomExceptionHandler.class
})
class FeatureFlagCustomExceptionHandlerIntegrationTest {

  @ControllerAdvice
  @Order(0)
  static class CustomExceptionHandler {

    @ExceptionHandler(FeatureFlagAccessDeniedException.class)
    ResponseEntity<String> handle(FeatureFlagAccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body("custom: " + e.featureName());
    }
  }

  WebTestClient webTestClient;

  @Test
  void customResolutionTakesPriority_whenClassLevelFeatureIsDisabled() {
    webTestClient
        .get()
        .uri("/test/disable")
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        .expectBody(String.class)
        .isEqualTo("custom: disable-class-level-feature");
  }

  @Test
  void customResolutionTakesPriority_whenMethodLevelFeatureIsDisabled() {
    webTestClient
        .get()
        .uri("/development-stage-endpoint")
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        .expectBody(String.class)
        .isEqualTo("custom: development-stage-endpoint");
  }

  @Autowired
  FeatureFlagCustomExceptionHandlerIntegrationTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }
}
