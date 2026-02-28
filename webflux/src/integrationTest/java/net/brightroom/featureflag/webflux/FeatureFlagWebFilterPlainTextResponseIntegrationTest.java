package net.brightroom.featureflag.webflux;

import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxTestAutoConfiguration;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagDisableController;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagEnableController;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagMethodLevelController;
import net.brightroom.featureflag.webflux.endpoint.NoFeatureFlagController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(
    properties = {"feature-flags.response.type=PLAIN_TEXT"},
    controllers = {
      NoFeatureFlagController.class,
      FeatureFlagEnableController.class,
      FeatureFlagDisableController.class,
      FeatureFlagMethodLevelController.class,
    })
@Import(FeatureFlagWebFluxTestAutoConfiguration.class)
class FeatureFlagWebFilterPlainTextResponseIntegrationTest {

  WebTestClient webTestClient;

  @Test
  void shouldAllowAccess_whenNoAnnotated() {
    webTestClient
        .get()
        .uri("/stable-endpoint")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("No Annotation");
  }

  @Test
  void shouldAllowAccess_whenFeatureIsEnabled() {
    webTestClient
        .get()
        .uri("/experimental-stage-endpoint")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Allowed");
  }

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
  void shouldAllowAccess_whenNoFeatureFlagAnnotationOnController() {
    webTestClient
        .get()
        .uri("/test/no-annotation")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("No Annotation");
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

  @Test
  void shouldAllowAccess_whenClassLevelFeatureIsEnabled() {
    webTestClient
        .get()
        .uri("/test/enabled")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Allowed");
  }

  @Autowired
  FeatureFlagWebFilterPlainTextResponseIntegrationTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }
}
