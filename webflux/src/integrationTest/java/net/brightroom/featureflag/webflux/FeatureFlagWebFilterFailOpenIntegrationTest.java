package net.brightroom.featureflag.webflux;

import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxTestAutoConfiguration;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagUndefinedFlagController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Verifies fail-open behavior: when {@code feature-flags.default-enabled} is {@code true}, requests
 * to endpoints whose flag is absent from {@code feature-flags.feature-names} are allowed through.
 */
@WebFluxTest(
    properties = {"feature-flags.default-enabled=true"},
    controllers = FeatureFlagUndefinedFlagController.class)
@Import(FeatureFlagWebFluxTestAutoConfiguration.class)
class FeatureFlagWebFilterFailOpenIntegrationTest {

  WebTestClient webTestClient;

  @Test
  void shouldAllowAccess_whenFlagIsUndefinedInConfig_andDefaultEnabledIsTrue() {
    webTestClient
        .get()
        .uri("/undefined-flag-endpoint")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Allowed");
  }

  @Autowired
  FeatureFlagWebFilterFailOpenIntegrationTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }
}
