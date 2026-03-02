package net.brightroom.featureflag.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import net.brightroom.featureflag.actuator.endpoint.FeatureFlagEndpointResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = {
      "management.endpoints.web.exposure.include=feature-flags",
      "feature-flags.feature-names.new-feature=true",
      "feature-flags.feature-names.disabled-feature=false"
    })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FeatureFlagEndpointWriteIntegrationTest {

  @LocalServerPort int port;

  WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void shouldDisableFeatureFlag_whenWriteOperationCalledWithEnabledFalse() {
    webTestClient
        .post()
        .uri("/actuator/feature-flags/new-feature")
        .contentType(MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json"))
        .bodyValue("{\"enabled\":false}")
        .exchange()
        .expectStatus()
        .isNoContent();

    webTestClient
        .get()
        .uri("/actuator/feature-flags")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(FeatureFlagEndpointResponse.class)
        .value(
            body -> {
              assertThat(body).isNotNull();
              assertThat(body.features().get("new-feature")).isFalse();
            });
  }

  @Test
  void shouldEnableFeatureFlag_whenWriteOperationCalledWithEnabledTrue() {
    webTestClient
        .post()
        .uri("/actuator/feature-flags/disabled-feature")
        .contentType(MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json"))
        .bodyValue("{\"enabled\":true}")
        .exchange()
        .expectStatus()
        .isNoContent();

    webTestClient
        .get()
        .uri("/actuator/feature-flags")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(FeatureFlagEndpointResponse.class)
        .value(
            body -> {
              assertThat(body).isNotNull();
              assertThat(body.features().get("disabled-feature")).isTrue();
            });
  }
}
