package net.brightroom.featureflag.webmvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.brightroom.featureflag.webmvc.configuration.FeatureFlagMvcTestAutoConfiguration;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagRouterConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies fail-open behavior for Functional Endpoints: when {@code feature-flags.default-enabled}
 * is {@code true}, requests to routes whose flag is absent from {@code feature-flags.features} are
 * allowed through.
 */
@WebMvcTest(properties = {"feature-flags.default-enabled=true"})
@Import({FeatureFlagMvcTestAutoConfiguration.class, FeatureFlagRouterConfiguration.class})
class FeatureFlagHandlerFilterFunctionFailOpenIntegrationTest {

  MockMvc mockMvc;

  @Test
  void shouldAllowAccess_whenFlagIsUndefinedInConfig_andDefaultEnabledIsTrue() throws Exception {
    mockMvc
        .perform(get("/functional/undefined-flag-endpoint"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }

  @Autowired
  FeatureFlagHandlerFilterFunctionFailOpenIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
