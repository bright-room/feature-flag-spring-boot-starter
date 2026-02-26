package net.brightroom.featureflag.webmvc.configuration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    properties = {"spring.mvc.problemdetails.enabled=true"},
    controllers = {
      NoFeatureFlagController.class,
      FeatureFlagEnableController.class,
      FeatureFlagDisableController.class,
      FeatureFlagMethodLevelController.class,
    })
@Import(FeatureFlagMvcTestAutoConfiguration.class)
class FeatureFlagInterceptorRFC7807JsonResponseIntegrationTest {

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
        .andExpect(status().isMethodNotAllowed())
        .andExpect(
            content()
                .json(
                    """

                  {
                    "detail" : "The feature flag is disabled",
                    "instance" : "/development-stage-endpoint",
                    "properties" : {
                      "error" : "This feature is not available"
                    },
                    "status" : 405,
                    "title" : "Access Denied",
                    "type" : "https://github.com/bright-room/feature-flag-spring-boot-starter"
                  }
                  """));
  }

  @Test
  void shouldBlockAccess_whenClassLevelFeatureNoAnnotated() throws Exception {
    mockMvc
        .perform(get("/test/no-annotation"))
        .andExpect(status().isOk())
        .andExpect(content().string("No Annotation"));
  }

  @Test
  void shouldBlockAccess_whenClassLevelFeatureIsDisabled() throws Exception {
    mockMvc
        .perform(get("/test/disable"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(
            content()
                .json(
                    """

                  {
                    "detail" : "The feature flag is disabled",
                    "instance" : "/test/disable",
                    "properties" : {
                      "error" : "This feature is not available"
                    },
                    "status" : 405,
                    "title" : "Access Denied",
                    "type" : "https://github.com/bright-room/feature-flag-spring-boot-starter"
                  }
                  """));
  }

  @Test
  void shouldAllowAccess_whenNoFeatureFlagAnnotation() throws Exception {
    mockMvc
        .perform(get("/test/enabled"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }

  @Autowired
  FeatureFlagInterceptorRFC7807JsonResponseIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
