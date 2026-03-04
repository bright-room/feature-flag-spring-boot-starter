package net.brightroom.featureflag.actuator;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import net.brightroom.featureflag.actuator.configuration.FeatureFlagActuatorTestAutoConfiguration;
import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
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
      "feature-flags.feature-names.feature-a=true",
      "feature-flags.feature-names.feature-b=false",
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
    mockMvc.perform(
        post("/actuator/feature-flags")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"featureName": "feature-a", "enabled": false}
                """));

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

  @Autowired
  FeatureFlagEndpointIntegrationTest(MockMvc mockMvc, EventCapture eventCapture) {
    this.mockMvc = mockMvc;
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
