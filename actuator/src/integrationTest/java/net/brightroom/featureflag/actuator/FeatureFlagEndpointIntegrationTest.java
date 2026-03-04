package net.brightroom.featureflag.actuator;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import net.brightroom.featureflag.actuator.configuration.FeatureFlagActuatorTestAutoConfiguration;
import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import net.brightroom.featureflag.core.event.FeatureFlagRemovedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = FeatureFlagActuatorTestAutoConfiguration.class)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "feature-flags.features.feature-a.enabled=true",
      "feature-flags.features.feature-a.rollout=50",
      "feature-flags.features.feature-b.enabled=false",
      "feature-flags.default-enabled=false",
      "management.endpoints.web.exposure.include=feature-flags"
    })
@Import(FeatureFlagEndpointIntegrationTest.EventCapture.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FeatureFlagEndpointIntegrationTest {

  MockMvc mockMvc;
  EventCapture eventCapture;

  @BeforeEach
  void resetEvents() {
    eventCapture.clear();
  }

  @Test
  void get_returnsAllFlagsAndDefaultEnabled() throws Exception {
    mockMvc
        .perform(get("/actuator/feature-flags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features[?(@.featureName == 'feature-a')].enabled", hasItem(true)))
        .andExpect(jsonPath("$.features[?(@.featureName == 'feature-b')].enabled", hasItem(false)))
        .andExpect(jsonPath("$.defaultEnabled").value(false));
  }

  @Test
  void post_updatesFlagAndReturnsUpdatedState() throws Exception {
    mockMvc
        .perform(
            post("/actuator/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"featureName": "feature-a", "enabled": false}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features[?(@.featureName == 'feature-a')].enabled", hasItem(false)));
  }

  @Test
  void post_thenGet_persistsUpdateInMemory() throws Exception {
    mockMvc
        .perform(
            post("/actuator/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"featureName": "feature-b", "enabled": true}
                    """))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/actuator/feature-flags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features[?(@.featureName == 'feature-b')].enabled", hasItem(true)));
  }

  @Test
  void post_addsNewFlagNotPreviouslyDefined() throws Exception {
    mockMvc
        .perform(
            post("/actuator/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"featureName": "new-flag", "enabled": true}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features[?(@.featureName == 'new-flag')].enabled", hasItem(true)));
  }

  @Test
  void post_publishesFeatureFlagChangedEvent() throws Exception {
    mockMvc
        .perform(
            post("/actuator/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"featureName": "feature-a", "enabled": false}
                    """))
        .andExpect(status().isOk());

    assertEquals(1, eventCapture.events().size());
    var event = eventCapture.events().get(0);
    assertEquals("feature-a", event.featureName());
    assertEquals(false, event.enabled());
    assertNotNull(event.getSource());
  }

  @Test
  void get_withSelector_returnsIndividualFlag() throws Exception {
    mockMvc
        .perform(get("/actuator/feature-flags/feature-a"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.featureName").value("feature-a"))
        .andExpect(jsonPath("$.enabled").value(true));
  }

  @Test
  void get_withSelector_returnsDefaultEnabled_whenFlagIsUndefined() throws Exception {
    mockMvc
        .perform(get("/actuator/feature-flags/undefined-flag"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.featureName").value("undefined-flag"))
        .andExpect(jsonPath("$.enabled").value(false));
  }

  @Test
  void post_thenGetWithSelector_reflectsUpdate() throws Exception {
    mockMvc
        .perform(
            post("/actuator/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"featureName": "feature-b", "enabled": true}
                    """))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/actuator/feature-flags/feature-b"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.featureName").value("feature-b"))
        .andExpect(jsonPath("$.enabled").value(true));
  }

  @Test
  void get_returnsRolloutPercentageForEachFlag() throws Exception {
    mockMvc
        .perform(get("/actuator/feature-flags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features[?(@.featureName == 'feature-a')].rollout", hasItem(50)))
        .andExpect(jsonPath("$.features[?(@.featureName == 'feature-b')].rollout", hasItem(100)));
  }

  @Test
  void get_withSelector_returnsRolloutPercentage() throws Exception {
    mockMvc
        .perform(get("/actuator/feature-flags/feature-a"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rollout").value(50));
  }

  @Test
  void post_withRollout_updatesRolloutPercentage() throws Exception {
    mockMvc
        .perform(
            post("/actuator/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"featureName": "feature-a", "enabled": true, "rollout": 80}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features[?(@.featureName == 'feature-a')].rollout", hasItem(80)));
  }

  @Test
  void post_withRollout_thenGet_persistsRolloutUpdate() throws Exception {
    mockMvc
        .perform(
            post("/actuator/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"featureName": "feature-a", "enabled": true, "rollout": 30}
                    """))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/actuator/feature-flags/feature-a"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rollout").value(30));
  }

  @Test
  void post_withRollout_publishesEventWithRolloutPercentage() throws Exception {
    mockMvc
        .perform(
            post("/actuator/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"featureName": "feature-a", "enabled": true, "rollout": 70}
                    """))
        .andExpect(status().isOk());

    assertEquals(1, eventCapture.events().size());
    var event = eventCapture.events().get(0);
    assertEquals("feature-a", event.featureName());
    assertEquals(true, event.enabled());
    assertEquals(70, event.rolloutPercentage());
  }

  @Test
  void delete_returnsNoContent() throws Exception {
    mockMvc.perform(delete("/actuator/feature-flags/feature-a")).andExpect(status().isNoContent());
  }

  @Test
  void delete_thenGet_flagIsRemoved() throws Exception {
    mockMvc.perform(delete("/actuator/feature-flags/feature-a")).andExpect(status().isNoContent());

    mockMvc
        .perform(get("/actuator/feature-flags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features[?(@.featureName == 'feature-a')]").isEmpty());
  }

  @Test
  void delete_thenGetWithSelector_returnsDefaultEnabled() throws Exception {
    mockMvc.perform(delete("/actuator/feature-flags/feature-a")).andExpect(status().isNoContent());

    mockMvc
        .perform(get("/actuator/feature-flags/feature-a"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.featureName").value("feature-a"))
        .andExpect(jsonPath("$.enabled").value(false));
  }

  @Test
  void delete_isIdempotent_forNonexistentFlag() throws Exception {
    mockMvc
        .perform(delete("/actuator/feature-flags/nonexistent"))
        .andExpect(status().isNoContent());
  }

  @Test
  void delete_publishesFeatureFlagRemovedEvent() throws Exception {
    mockMvc.perform(delete("/actuator/feature-flags/feature-a")).andExpect(status().isNoContent());

    assertEquals(1, eventCapture.removedEvents().size());
    var event = eventCapture.removedEvents().get(0);
    assertEquals("feature-a", event.featureName());
    assertNotNull(event.getSource());
    assertTrue(eventCapture.events().isEmpty());
  }

  @Autowired
  FeatureFlagEndpointIntegrationTest(MockMvc mockMvc, EventCapture eventCapture) {
    this.mockMvc = mockMvc;
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
