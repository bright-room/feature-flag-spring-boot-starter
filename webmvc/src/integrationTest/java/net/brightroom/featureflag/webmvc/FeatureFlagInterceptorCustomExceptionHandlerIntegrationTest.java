package net.brightroom.featureflag.webmvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.webmvc.configuration.FeatureFlagMvcTestAutoConfiguration;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagDisableController;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagMethodLevelController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@WebMvcTest(controllers = {FeatureFlagDisableController.class, FeatureFlagMethodLevelController.class})
@Import({
  FeatureFlagMvcTestAutoConfiguration.class,
  FeatureFlagInterceptorCustomExceptionHandlerIntegrationTest.CustomExceptionHandler.class
})
class FeatureFlagInterceptorCustomExceptionHandlerIntegrationTest {

  @ControllerAdvice
  @Order(0)
  static class CustomExceptionHandler {

    @ExceptionHandler(FeatureFlagAccessDeniedException.class)
    ResponseEntity<String> handle(FeatureFlagAccessDeniedException e) {
      return ResponseEntity.status(503).body("custom: " + e.featureName());
    }
  }

  MockMvc mockMvc;

  @Test
  void customHandlerTakesPriority_whenFeatureIsDisabled() throws Exception {
    mockMvc
        .perform(get("/test/disable"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().string("custom: disable-class-level-feature"));
  }

  @Test
  void customHandlerTakesPriority_whenMethodLevelFeatureIsDisabled() throws Exception {
    mockMvc
        .perform(get("/development-stage-endpoint"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().string("custom: development-stage-endpoint"));
  }

  @Autowired
  FeatureFlagInterceptorCustomExceptionHandlerIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
