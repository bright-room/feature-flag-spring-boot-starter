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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import({FeatureFlagMvcTestAutoConfiguration.class, FeatureFlagRouterConfiguration.class})
class FeatureFlagHandlerFilterFunctionJsonResponseIntegrationTest {

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
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "detail" : "Feature 'development-stage-endpoint' is not available",
                      "instance" : "/functional/development-stage-endpoint",
                      "status" : 403,
                      "title" : "Feature flag access denied",
                      "type" : "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"
                    }
                    """));
  }

  @Test
  void shouldBlockAccess_whenGroupedRouteFeatureIsDisabled() throws Exception {
    mockMvc
        .perform(get("/functional/test/disable"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "detail" : "Feature 'disable-class-level-feature' is not available",
                      "instance" : "/functional/test/disable",
                      "status" : 403,
                      "title" : "Feature flag access denied",
                      "type" : "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"
                    }
                    """));
  }

  @Test
  void shouldAllowAccess_whenGroupedRouteFeatureIsEnabled() throws Exception {
    mockMvc
        .perform(get("/functional/test/enabled"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }

  @Autowired
  FeatureFlagHandlerFilterFunctionJsonResponseIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
