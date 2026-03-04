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

@WebMvcTest(properties = {"feature-flags.response.type=PLAIN_TEXT"})
@Import({FeatureFlagMvcTestAutoConfiguration.class, FeatureFlagRouterConfiguration.class})
class FeatureFlagHandlerFilterFunctionPlainTextResponseIntegrationTest {

  MockMvc mockMvc;

  @Test
  void shouldAllowAccess_whenNoFilter() throws Exception {
    mockMvc
        .perform(get("/functional/stable-endpoint"))
        .andExpect(status().isOk())
        .andExpect(content().string("No Annotation"));
  }

  @Test
  void shouldAllowAccess_whenFeatureIsEnabled() throws Exception {
    mockMvc
        .perform(get("/functional/experimental-stage-endpoint"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }

  @Test
  void shouldBlockAccess_whenFeatureIsDisabled() throws Exception {
    mockMvc
        .perform(get("/functional/development-stage-endpoint"))
        .andExpect(status().isForbidden())
        .andExpect(content().string("Feature 'development-stage-endpoint' is not available"));
  }

  @Test
  void shouldBlockAccess_whenClassLevelFeatureIsDisabled() throws Exception {
    mockMvc
        .perform(get("/functional/test/disable"))
        .andExpect(status().isForbidden())
        .andExpect(content().string("Feature 'disable-class-level-feature' is not available"));
  }

  @Test
  void shouldAllowAccess_whenClassLevelFeatureIsEnabled() throws Exception {
    mockMvc
        .perform(get("/functional/test/enabled"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }

  @Autowired
  FeatureFlagHandlerFilterFunctionPlainTextResponseIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
