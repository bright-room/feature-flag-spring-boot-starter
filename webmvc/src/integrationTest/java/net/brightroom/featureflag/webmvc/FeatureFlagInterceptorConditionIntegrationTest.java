package net.brightroom.featureflag.webmvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.brightroom.featureflag.webmvc.configuration.FeatureFlagMvcTestAutoConfiguration;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagConditionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FeatureFlagConditionController.class)
@Import(FeatureFlagMvcTestAutoConfiguration.class)
@TestPropertySource(
    properties = {
      "feature-flags.features.conditional-feature.enabled=true",
      "feature-flags.features.conditional-feature.rollout=100",
    })
class FeatureFlagInterceptorConditionIntegrationTest {

  MockMvc mockMvc;

  @Test
  void shouldAllowAccess_whenHeaderConditionIsSatisfied() throws Exception {
    mockMvc
        .perform(get("/condition/header").header("X-Beta", "true"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }

  @Test
  void shouldBlockAccess_whenHeaderConditionIsNotSatisfied() throws Exception {
    mockMvc
        .perform(get("/condition/header"))
        .andExpect(status().isForbidden())
        .andExpect(
            content()
                .json(
                    """
                  {
                    "detail" : "Feature 'conditional-feature' is not available",
                    "instance" : "/condition/header",
                    "status" : 403,
                    "title" : "Feature flag access denied",
                    "type" : "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"
                  }
                  """));
  }

  @Test
  void shouldAllowAccess_whenParamConditionIsSatisfied() throws Exception {
    mockMvc
        .perform(get("/condition/param").param("variant", "B"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }

  @Test
  void shouldBlockAccess_whenParamConditionIsNotSatisfied() throws Exception {
    mockMvc
        .perform(get("/condition/param").param("variant", "A"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldBlockAccess_whenParamIsMissing() throws Exception {
    mockMvc.perform(get("/condition/param")).andExpect(status().isForbidden());
  }

  @Test
  void shouldBlockAccess_onConditionWithRollout_whenConditionIsNotSatisfied() throws Exception {
    // condition fails (no X-Beta header) — access denied regardless of rollout
    mockMvc.perform(get("/condition/with-rollout")).andExpect(status().isForbidden());
  }

  @Test
  void shouldAllowAccess_onConditionWithRollout_whenConditionSatisfiedAndRolloutFull()
      throws Exception {
    // rollout overridden to 100% via property, condition satisfied
    mockMvc
        .perform(get("/condition/with-rollout").header("X-Beta", "true"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }

  @Test
  void shouldAllowAccess_whenRemoteAddressConditionIsSatisfied() throws Exception {
    // MockMvc defaults remoteAddr to 127.0.0.1
    mockMvc
        .perform(get("/condition/remote-address"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }

  @Autowired
  FeatureFlagInterceptorConditionIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
