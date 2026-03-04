package net.brightroom.featureflag.webmvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.brightroom.featureflag.webmvc.configuration.FeatureFlagMvcTestAutoConfiguration;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagRouterConfiguration;
import net.brightroom.featureflag.webmvc.resolution.handlerfilter.AccessDeniedHandlerFilterResolution;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.function.ServerResponse;

@WebMvcTest
@Import({
  FeatureFlagHandlerFilterFunctionCustomResolutionIntegrationTest.CustomResolutionConfiguration
      .class,
  FeatureFlagMvcTestAutoConfiguration.class,
  FeatureFlagRouterConfiguration.class,
})
class FeatureFlagHandlerFilterFunctionCustomResolutionIntegrationTest {

  @Configuration
  static class CustomResolutionConfiguration {

    @Bean
    @Primary
    AccessDeniedHandlerFilterResolution customResolution() {
      return (request, e) ->
          ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("custom: " + e.featureName());
    }
  }

  MockMvc mockMvc;

  @Test
  void customResolutionTakesPriority_whenFeatureIsDisabled() throws Exception {
    mockMvc
        .perform(get("/functional/development-stage-endpoint"))
        .andExpect(status().is(HttpStatus.SERVICE_UNAVAILABLE.value()))
        .andExpect(content().string("custom: development-stage-endpoint"));
  }

  @Test
  void customResolutionTakesPriority_whenClassLevelFeatureIsDisabled() throws Exception {
    mockMvc
        .perform(get("/functional/test/disable"))
        .andExpect(status().is(HttpStatus.SERVICE_UNAVAILABLE.value()))
        .andExpect(content().string("custom: disable-class-level-feature"));
  }

  @Autowired
  FeatureFlagHandlerFilterFunctionCustomResolutionIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
