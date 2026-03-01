package net.brightroom.featureflag.webflux;

import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxTestAutoConfiguration;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagDisableController;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagMethodLevelController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(
    properties = {"feature-flags.response.type=PLAIN_TEXT"},
    controllers = {
      FeatureFlagDisableController.class,
      FeatureFlagMethodLevelController.class,
    })
@Import(FeatureFlagWebFluxTestAutoConfiguration.class)
class FeatureFlagAspectPlainTextResponseIntegrationTest {

  WebTestClient webTestClient;

  @Test
  void shouldBlockAccess_whenFeatureIsDisabled() {
    webTestClient
        .get()
        .uri("/development-stage-endpoint")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody(String.class)
        .isEqualTo("Feature 'development-stage-endpoint' is not available");
  }

  @Test
  void shouldBlockAccess_whenClassLevelFeatureIsDisabled() {
    webTestClient
        .get()
        .uri("/test/disable")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody(String.class)
        .isEqualTo("Feature 'disable-class-level-feature' is not available");
  }

  @Autowired
  FeatureFlagAspectPlainTextResponseIntegrationTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }
}
