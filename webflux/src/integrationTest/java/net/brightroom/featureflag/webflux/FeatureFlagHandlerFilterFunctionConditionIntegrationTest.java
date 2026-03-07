package net.brightroom.featureflag.webflux;

import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxTestAutoConfiguration;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagRouterConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest
@Import({FeatureFlagWebFluxTestAutoConfiguration.class, FeatureFlagRouterConfiguration.class})
@TestPropertySource(
    properties = {
      "feature-flags.features.conditional-feature.enabled=true",
    })
class FeatureFlagHandlerFilterFunctionConditionIntegrationTest {

  WebTestClient webTestClient;

  @Test
  void shouldAllowAccess_whenHeaderConditionIsSatisfied() {
    webTestClient
        .get()
        .uri("/functional/condition/header")
        .header("X-Beta", "true")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Allowed");
  }

  @Test
  void shouldBlockAccess_whenHeaderConditionIsNotSatisfied() {
    webTestClient
        .get()
        .uri("/functional/condition/header")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .json(
            """
            {
              "detail" : "Feature 'conditional-feature' is not available",
              "instance" : "/functional/condition/header",
              "status" : 403,
              "title" : "Feature flag access denied",
              "type" : "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"
            }
            """);
  }

  @Autowired
  FeatureFlagHandlerFilterFunctionConditionIntegrationTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }
}
