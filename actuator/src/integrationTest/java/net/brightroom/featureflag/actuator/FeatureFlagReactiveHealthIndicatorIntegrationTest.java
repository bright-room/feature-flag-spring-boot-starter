package net.brightroom.featureflag.actuator;

import net.brightroom.featureflag.actuator.configuration.FeatureFlagActuatorTestAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    classes = FeatureFlagActuatorTestAutoConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.main.web-application-type=reactive",
      "feature-flags.features.feature-a.enabled=true",
      "feature-flags.features.feature-b.enabled=false",
      "feature-flags.default-enabled=false",
      "management.endpoints.web.exposure.include=health",
      "management.endpoint.health.show-details=always"
    })
class FeatureFlagReactiveHealthIndicatorIntegrationTest {

  @LocalServerPort int port;

  WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void health_containsFeatureFlagComponent() {
    webTestClient
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("UP")
        .jsonPath("$.components.featureFlag.status")
        .isEqualTo("UP")
        .jsonPath("$.components.featureFlag.details.provider")
        .isEqualTo("MutableInMemoryReactiveFeatureFlagProvider")
        .jsonPath("$.components.featureFlag.details.totalFlags")
        .isEqualTo(2)
        .jsonPath("$.components.featureFlag.details.enabledFlags")
        .isEqualTo(1)
        .jsonPath("$.components.featureFlag.details.disabledFlags")
        .isEqualTo(1)
        .jsonPath("$.components.featureFlag.details.defaultEnabled")
        .isEqualTo(false);
  }
}
