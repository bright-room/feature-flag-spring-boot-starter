package net.brightroom.featureflag.webflux;

import net.brightroom.featureflag.webflux.configuration.FeatureFlagWebFluxTestAutoConfiguration;
import net.brightroom.featureflag.webflux.endpoint.FeatureFlagConditionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = FeatureFlagConditionController.class)
@Import(FeatureFlagWebFluxTestAutoConfiguration.class)
@TestPropertySource(
    properties = {
      "feature-flags.features.conditional-feature.enabled=true",
      "feature-flags.features.conditional-feature.rollout=100",
    })
class FeatureFlagAspectConditionIntegrationTest {

  WebTestClient webTestClient;

  @Test
  void shouldAllowAccess_whenHeaderConditionIsSatisfied() {
    webTestClient
        .get()
        .uri("/condition/header")
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
        .uri("/condition/header")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .json(
            """
            {
              "detail" : "Feature 'conditional-feature' is not available",
              "instance" : "/condition/header",
              "status" : 403,
              "title" : "Feature flag access denied",
              "type" : "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"
            }
            """);
  }

  @Test
  void shouldAllowAccess_whenParamConditionIsSatisfied() {
    webTestClient
        .get()
        .uri("/condition/param?variant=B")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Allowed");
  }

  @Test
  void shouldBlockAccess_whenParamConditionIsNotSatisfied() {
    webTestClient.get().uri("/condition/param?variant=A").exchange().expectStatus().isForbidden();
  }

  @Test
  void shouldBlockAccess_whenParamIsMissing() {
    webTestClient.get().uri("/condition/param").exchange().expectStatus().isForbidden();
  }

  @Test
  void shouldBlockAccess_onConditionWithRollout_whenConditionIsNotSatisfied() {
    // condition fails (no X-Beta header) — access denied regardless of rollout
    webTestClient.get().uri("/condition/with-rollout").exchange().expectStatus().isForbidden();
  }

  @Test
  void shouldAllowAccess_onConditionWithRollout_whenConditionSatisfiedAndRolloutFull() {
    // rollout overridden to 100% via property, condition satisfied
    webTestClient
        .get()
        .uri("/condition/with-rollout")
        .header("X-Beta", "true")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Allowed");
  }

  @Test
  void shouldBlockAccess_whenRemoteAddressConditionIsNotSatisfied() {
    // @WebFluxTest slice does not set a real remote address, so the condition fails
    webTestClient.get().uri("/condition/remote-address").exchange().expectStatus().isForbidden();
  }

  @Autowired
  FeatureFlagAspectConditionIntegrationTest(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }
}
