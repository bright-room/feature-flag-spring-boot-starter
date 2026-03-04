package net.brightroom.featureflag.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.brightroom.featureflag.actuator.configuration.FeatureFlagActuatorTestAutoConfiguration;
import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import net.brightroom.featureflag.core.event.FeatureFlagRemovedEvent;
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
      "feature-flags.features.feature-a.rollout=50",
      "feature-flags.features.feature-b.enabled=false",
      "feature-flags.default-enabled=false",
      "management.endpoints.web.exposure.include=feature-flags"
    })
@Import(FeatureFlagReactiveEndpointIntegrationTest.EventCapture.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SuppressWarnings("unchecked")
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
        .value(v -> assertThat((List<Object>) v).contains(true))
        .jsonPath("$.features[?(@.featureName == 'feature-b')].enabled")
        .value(v -> assertThat((List<Object>) v).contains(false))
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
        .value(v -> assertThat((List<Object>) v).contains(false));
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
        .value(v -> assertThat((List<Object>) v).contains(true));
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
        .value(v -> assertThat((List<Object>) v).contains(true));
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

  @Test
  void get_returnsRolloutPercentageForEachFlag() {
    webTestClient
        .get()
        .uri("/actuator/feature-flags")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.features[?(@.featureName == 'feature-a')].rollout")
        .value(v -> assertThat((List<Object>) v).contains(50))
        .jsonPath("$.features[?(@.featureName == 'feature-b')].rollout")
        .value(v -> assertThat((List<Object>) v).contains(100));
  }

  @Test
  void get_withSelector_returnsRolloutPercentage() {
    webTestClient
        .get()
        .uri("/actuator/feature-flags/feature-a")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.rollout")
        .isEqualTo(50);
  }

  @Test
  void post_withRollout_updatesRolloutPercentage() {
    webTestClient
        .post()
        .uri("/actuator/feature-flags")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"featureName": "feature-a", "enabled": true, "rollout": 80}
            """)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.features[?(@.featureName == 'feature-a')].rollout")
        .value(v -> assertThat((List<Object>) v).contains(80));
  }

  @Test
  void post_withRollout_thenGet_persistsRolloutUpdate() {
    webTestClient
        .post()
        .uri("/actuator/feature-flags")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"featureName": "feature-a", "enabled": true, "rollout": 30}
            """)
        .exchange()
        .expectStatus()
        .isOk();

    webTestClient
        .get()
        .uri("/actuator/feature-flags/feature-a")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.rollout")
        .isEqualTo(30);
  }

  @Test
  void post_withRollout_publishesEventWithRolloutPercentage() {
    webTestClient
        .post()
        .uri("/actuator/feature-flags")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"featureName": "feature-a", "enabled": true, "rollout": 70}
            """)
        .exchange()
        .expectStatus()
        .isOk();

    assertEquals(1, eventCapture.events().size());
    var event = eventCapture.events().get(0);
    assertEquals("feature-a", event.featureName());
    assertEquals(true, event.enabled());
    assertEquals(70, event.rolloutPercentage());
  }

  @Test
  void delete_returnsNoContent() {
    webTestClient
        .delete()
        .uri("/actuator/feature-flags/feature-a")
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  @Test
  void delete_thenGet_flagIsRemoved() {
    webTestClient
        .delete()
        .uri("/actuator/feature-flags/feature-a")
        .exchange()
        .expectStatus()
        .isNoContent();

    webTestClient
        .get()
        .uri("/actuator/feature-flags")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.features[?(@.featureName == 'feature-a')]")
        .isEmpty();
  }

  @Test
  void delete_thenGetWithSelector_returnsDefaultEnabled() {
    webTestClient
        .delete()
        .uri("/actuator/feature-flags/feature-a")
        .exchange()
        .expectStatus()
        .isNoContent();

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
        .isEqualTo(false);
  }

  @Test
  void delete_isIdempotent_forNonexistentFlag() {
    webTestClient
        .delete()
        .uri("/actuator/feature-flags/nonexistent")
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  @Test
  void delete_publishesFeatureFlagRemovedEvent() {
    webTestClient
        .delete()
        .uri("/actuator/feature-flags/feature-a")
        .exchange()
        .expectStatus()
        .isNoContent();

    assertEquals(1, eventCapture.removedEvents().size());
    var event = eventCapture.removedEvents().get(0);
    assertEquals("feature-a", event.featureName());
    assertNotNull(event.getSource());
    assertTrue(eventCapture.events().isEmpty());
  }

  @Autowired
  FeatureFlagReactiveEndpointIntegrationTest(EventCapture eventCapture) {
    this.eventCapture = eventCapture;
  }

  @Component
  static class EventCapture {

    private final List<FeatureFlagChangedEvent> captured = new ArrayList<>();
    private final List<FeatureFlagRemovedEvent> capturedRemoved = new ArrayList<>();

    @EventListener
    void onEvent(FeatureFlagChangedEvent event) {
      captured.add(event);
    }

    @EventListener
    void onEvent(FeatureFlagRemovedEvent event) {
      capturedRemoved.add(event);
    }

    List<FeatureFlagChangedEvent> events() {
      return List.copyOf(captured);
    }

    List<FeatureFlagRemovedEvent> removedEvents() {
      return List.copyOf(capturedRemoved);
    }

    void clear() {
      captured.clear();
      capturedRemoved.clear();
    }
  }
}
