package net.brightroom.featureflag.webflux;

import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxTestAutoConfiguration;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagRouterConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Verifies fail-closed behavior for Functional Endpoints: when {@code
 * feature-flags.default-enabled} is {@code false}, requests to routes whose flag is absent from
 * {@code feature-flags.feature-names} are blocked with {@code 403 Forbidden}.
 */
@WebFluxTest(properties = {"feature-flags.default-enabled=false"})
@Import({FeatureFlagWebFluxTestAutoConfiguration.class, FeatureFlagRouterConfiguration.class})
class FeatureFlagHandlerFilterFunctionFailClosedIntegrationTest {

  WebTestClient webTestClient;

  @Test
  void shouldBlockAccess_whenFlagIsUndefinedInConfig_andDefaultEnabledIsFalse() {
    webTestClient
        .get()
        .uri("/functional/undefined-flag-endpoint")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .json(
            """
            {
              "detail"   : "Feature 'undefined-in-config-flag' is not available",
              "instance" : "/functional/undefined-flag-endpoint",
              "status"   : 403,
              "title"    : "Feature flag access denied",
              "type"     : "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"
            }
            """);
  }

  @Autowired
  FeatureFlagHandlerFilterFunctionFailClosedIntegrationTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }
}
