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
    controllers = {
      NoFeatureFlagController.class,
      FeatureFlagEnableController.class,
      FeatureFlagDisableController.class,
      FeatureFlagMethodLevelController.class,
    })
@Import(FeatureFlagMvcTestAutoConfiguration.class)
class FeatureFlagInterceptorJsonResponseIntegrationTest {

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
        .andExpect(
            content()
                .json(
                    """
                  {
                    "detail" : "Feature 'development-stage-endpoint' is not available",
                    "instance" : "/development-stage-endpoint",
                    "status" : 403,
                    "title" : "Feature flag access denied",
                    "type" : "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"
                  }
                  """));
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
        .andExpect(
            content()
                .json(
                    """
                  {
                    "detail" : "Feature 'disable-class-level-feature' is not available",
                    "instance" : "/test/disable",
                    "status" : 403,
                    "title" : "Feature flag access denied",
                    "type" : "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"
                  }
                  """));
  }

  @Test
  void shouldAllowAccess_whenClassLevelFeatureIsEnabled() throws Exception {
    mockMvc
        .perform(get("/test/enabled"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }

  @Autowired
  FeatureFlagInterceptorJsonResponseIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
