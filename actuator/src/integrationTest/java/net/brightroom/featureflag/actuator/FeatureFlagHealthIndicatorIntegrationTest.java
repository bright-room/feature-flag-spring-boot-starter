package net.brightroom.featureflag.actuator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.brightroom.featureflag.actuator.configuration.FeatureFlagActuatorTestAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = FeatureFlagActuatorTestAutoConfiguration.class)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "feature-flags.features.feature-a.enabled=true",
      "feature-flags.features.feature-b.enabled=false",
      "feature-flags.default-enabled=false",
      "management.endpoints.web.exposure.include=health",
      "management.endpoint.health.show-details=always"
    })
class FeatureFlagHealthIndicatorIntegrationTest {

  MockMvc mockMvc;

  @Autowired
  FeatureFlagHealthIndicatorIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void health_containsFeatureFlagComponent() throws Exception {
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components.featureFlag.status").value("UP"))
        .andExpect(
            jsonPath("$.components.featureFlag.details.provider")
                .value("MutableInMemoryFeatureFlagProvider"))
        .andExpect(jsonPath("$.components.featureFlag.details.totalFlags").value(2))
        .andExpect(jsonPath("$.components.featureFlag.details.enabledFlags").value(1))
        .andExpect(jsonPath("$.components.featureFlag.details.disabledFlags").value(1))
        .andExpect(jsonPath("$.components.featureFlag.details.defaultEnabled").value(false));
  }
}
