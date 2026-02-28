package net.brightroom.featureflag.webflux;

import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFilterCustomResolutionConfig;
import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxTestAutoConfiguration;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagDisableController;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagMethodLevelController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(
    controllers = {FeatureFlagDisableController.class, FeatureFlagMethodLevelController.class})
@Import({
  FeatureFlagWebFilterCustomResolutionConfig.class,
  FeatureFlagWebFluxTestAutoConfiguration.class
})
class FeatureFlagWebFilterCustomResolutionIntegrationTest {

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
  FeatureFlagWebFilterCustomResolutionIntegrationTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }
}
