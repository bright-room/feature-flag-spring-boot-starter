package net.brightroom.featureflag.webflux;

import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxTestAutoConfiguration;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagRouterConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(properties = {"feature-flags.response.type=PLAIN_TEXT"})
@Import({FeatureFlagWebFluxTestAutoConfiguration.class, FeatureFlagRouterConfiguration.class})
class FeatureFlagHandlerFilterFunctionPlainTextResponseIntegrationTest {

  WebTestClient webTestClient;

  @Test
  void shouldAllowAccess_whenNoFilter() {
    webTestClient
        .get()
        .uri("/functional/stable-endpoint")
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
        .uri("/functional/experimental-stage-endpoint")
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
        .uri("/functional/development-stage-endpoint")
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
        .uri("/functional/test/disable")
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
        .uri("/functional/test/enabled")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Allowed");
  }

  @Autowired
  FeatureFlagHandlerFilterFunctionPlainTextResponseIntegrationTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }
}
