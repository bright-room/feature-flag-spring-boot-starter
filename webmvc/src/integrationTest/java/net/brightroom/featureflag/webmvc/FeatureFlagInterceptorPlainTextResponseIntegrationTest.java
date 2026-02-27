package net.brightroom.featureflag.webmvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.brightroom.featureflag.webmvc.configuration.FeatureFlagMvcTestAutoConfiguration;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagDisableController;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagEnableController;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagMethodLevelController;
import net.brightroom.featureflag.webmvc.endpoint.NoFeatureFlagController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    properties = {"feature-flags.response.type=PLAIN_TEXT"},
    controllers = {
      NoFeatureFlagController.class,
      FeatureFlagEnableController.class,
      FeatureFlagDisableController.class,
      FeatureFlagMethodLevelController.class,
    })
@Import(FeatureFlagMvcTestAutoConfiguration.class)
class FeatureFlagInterceptorPlainTextResponseIntegrationTest {

  MockMvc mockMvc;

  @Test
  void shouldAllowAccess_whenNoAnnotated() throws Exception {
    mockMvc
        .perform(get("/stable-endpoint"))
        .andExpect(status().isOk())
        .andExpect(content().string("No Annotation"));
  }

  @Test
  void shouldAllowAccess_whenFeatureIsEnabled() throws Exception {
    mockMvc
        .perform(get("/experimental-stage-endpoint"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }

  @Test
  void shouldBlockAccess_whenFeatureIsDisabled() throws Exception {
    mockMvc
        .perform(get("/development-stage-endpoint"))
        .andExpect(status().isForbidden())
        .andExpect(content().string("Feature 'development-stage-endpoint' is not available"));
  }

  @Test
  void shouldAllowAccess_whenNoFeatureFlagAnnotationOnController() throws Exception {
    mockMvc
        .perform(get("/test/no-annotation"))
        .andExpect(status().isOk())
        .andExpect(content().string("No Annotation"));
  }

  @Test
  void shouldBlockAccess_whenClassLevelFeatureIsDisabled() throws Exception {
    mockMvc
        .perform(get("/test/disable"))
        .andExpect(status().isForbidden())
        .andExpect(content().string("Feature 'disable-class-level-feature' is not available"));
  }

  @Test
  void shouldAllowAccess_whenNoFeatureFlagAnnotation() throws Exception {
    mockMvc
        .perform(get("/test/enabled"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }

  @Autowired
  FeatureFlagInterceptorPlainTextResponseIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
