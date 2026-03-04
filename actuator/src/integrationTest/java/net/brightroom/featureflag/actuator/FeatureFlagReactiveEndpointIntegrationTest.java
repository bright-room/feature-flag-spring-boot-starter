package net.brightroom.featureflag.actuator;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import net.brightroom.featureflag.actuator.configuration.FeatureFlagActuatorTestAutoConfiguration;
import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    classes = FeatureFlagActuatorTestAutoConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.main.web-application-type=reactive",
      "feature-flags.features.feature-a.enabled=true",
      "feature-flags.features.feature-b.enabled=false",
      "feature-flags.default-enabled=false",
      "management.endpoints.web.exposure.include=feature-flags"
    })
@Import(FeatureFlagReactiveEndpointIntegrationTest.EventCapture.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FeatureFlagReactiveEndpointIntegrationTest {

  @LocalServerPort int port;

  WebTestClient webTestClient;
  EventCapture eventCapture;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    eventCapture.clear();
  }

  @Test
  void get_returnsAllFlagsAndDefaultEnabled() {
    webTestClient
        .get()
        .uri("/actuator/feature-flags")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.features[?(@.featureName == 'feature-a')].enabled")
        .value(hasItem(true))
        .jsonPath("$.features[?(@.featureName == 'feature-b')].enabled")
        .value(hasItem(false))
        .jsonPath("$.defaultEnabled")
        .isEqualTo(false);
  }

  @Test
  void post_updatesFlagAndReturnsUpdatedState() {
    webTestClient
        .post()
        .uri("/actuator/feature-flags")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"featureName": "feature-a", "enabled": false}
            """)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.features[?(@.featureName == 'feature-a')].enabled")
        .value(hasItem(false));
  }

  @Test
  void post_thenGet_persistsUpdateInMemory() {
    webTestClient
        .post()
        .uri("/actuator/feature-flags")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"featureName": "feature-b", "enabled": true}
            """)
        .exchange()
        .expectStatus()
        .isOk();

    webTestClient
        .get()
        .uri("/actuator/feature-flags")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.features[?(@.featureName == 'feature-b')].enabled")
        .value(hasItem(true));
  }

  @Test
  void post_addsNewFlagNotPreviouslyDefined() {
    webTestClient
        .post()
        .uri("/actuator/feature-flags")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"featureName": "new-flag", "enabled": true}
            """)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.features[?(@.featureName == 'new-flag')].enabled")
        .value(hasItem(true));
  }

  @Test
  void post_publishesFeatureFlagChangedEvent() {
    webTestClient
        .post()
        .uri("/actuator/feature-flags")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"featureName": "feature-a", "enabled": false}
            """)
        .exchange()
        .expectStatus()
        .isOk();

    assertEquals(1, eventCapture.events().size());
    var event = eventCapture.events().get(0);
    assertEquals("feature-a", event.featureName());
    assertEquals(false, event.enabled());
    assertNotNull(event.getSource());
  }

  @Test
  void get_withSelector_returnsIndividualFlag() {
    webTestClient
        .get()
        .uri("/actuator/feature-flags/feature-a")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.featureName")
        .isEqualTo("feature-a")
        .jsonPath("$.enabled")
        .isEqualTo(true);
  }

  @Test
  void get_withSelector_returnsDefaultEnabled_whenFlagIsUndefined() {
    webTestClient
        .get()
        .uri("/actuator/feature-flags/undefined-flag")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.featureName")
        .isEqualTo("undefined-flag")
        .jsonPath("$.enabled")
        .isEqualTo(false);
  }

  @Test
  void post_thenGetWithSelector_reflectsUpdate() {
    webTestClient
        .post()
        .uri("/actuator/feature-flags")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"featureName": "feature-b", "enabled": true}
            """)
        .exchange()
        .expectStatus()
        .isOk();

    webTestClient
        .get()
        .uri("/actuator/feature-flags/feature-b")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.featureName")
        .isEqualTo("feature-b")
        .jsonPath("$.enabled")
        .isEqualTo(true);
  }

  @Autowired
  FeatureFlagReactiveEndpointIntegrationTest(EventCapture eventCapture) {
    this.eventCapture = eventCapture;
  }

  @Component
  static class EventCapture {

    private final List<FeatureFlagChangedEvent> captured = new ArrayList<>();

    @EventListener
    void onEvent(FeatureFlagChangedEvent event) {
      captured.add(event);
    }

    List<FeatureFlagChangedEvent> events() {
      return List.copyOf(captured);
    }

    void clear() {
      captured.clear();
    }
  }
}
