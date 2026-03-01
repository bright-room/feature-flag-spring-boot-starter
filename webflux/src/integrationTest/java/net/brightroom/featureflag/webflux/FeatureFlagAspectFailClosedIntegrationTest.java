package net.brightroom.featureflag.webflux;

import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxTestAutoConfiguration;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagUndefinedFlagController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Verifies fail-closed behavior: when {@code feature-flags.default-enabled} is {@code false},
 * requests to endpoints whose flag is absent from {@code feature-flags.feature-names} are blocked
 * with {@code 403 Forbidden}.
 */
@WebFluxTest(
    properties = {"feature-flags.default-enabled=false"},
    controllers = FeatureFlagUndefinedFlagController.class)
@Import(FeatureFlagWebFluxTestAutoConfiguration.class)
class FeatureFlagAspectFailClosedIntegrationTest {

  WebTestClient webTestClient;

  @Test
  void shouldBlockAccess_whenFlagIsUndefinedInConfig_andDefaultEnabledIsFalse() {
    webTestClient
        .get()
        .uri("/undefined-flag-endpoint")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .json(
            """
            {
              "detail"   : "Feature 'undefined-in-config-flag' is not available",
              "instance" : "/undefined-flag-endpoint",
              "status"   : 403,
              "title"    : "Feature flag access denied",
              "type"     : "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"
            }
            """);
  }

  @Autowired
  FeatureFlagAspectFailClosedIntegrationTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }
}
