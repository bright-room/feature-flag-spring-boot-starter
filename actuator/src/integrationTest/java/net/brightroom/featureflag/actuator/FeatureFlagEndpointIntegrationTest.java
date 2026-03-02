package net.brightroom.featureflag.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import net.brightroom.featureflag.actuator.endpoint.FeatureFlagEndpointResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
class FeatureFlagEndpointIntegrationTest {

  @LocalServerPort int port;

  WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void shouldReturnOk_whenGetEndpointCalled() {
    webTestClient.get().uri("/actuator/feature-flags").exchange().expectStatus().isOk();
  }

  @Test
  void shouldReturnAllFeatureFlags_whenGetEndpointCalled() {
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
              assertThat(body.features())
                  .containsExactlyInAnyOrderEntriesOf(
                      Map.of("new-feature", true, "disabled-feature", false));
            });
  }
}
