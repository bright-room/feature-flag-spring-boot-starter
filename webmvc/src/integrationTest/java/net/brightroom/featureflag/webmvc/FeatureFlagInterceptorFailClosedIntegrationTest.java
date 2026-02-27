package net.brightroom.featureflag.webmvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.brightroom.featureflag.webmvc.configuration.FeatureFlagMvcTestAutoConfiguration;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagUndefinedFlagController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies fail-closed behavior: when {@code feature-flags.default-enabled} is {@code false},
 * requests to endpoints whose flag is absent from {@code feature-flags.feature-names} are blocked
 * with {@code 403 Forbidden}.
 */
@WebMvcTest(
    properties = {"feature-flags.default-enabled=false"},
    controllers = FeatureFlagUndefinedFlagController.class)
@Import(FeatureFlagMvcTestAutoConfiguration.class)
class FeatureFlagInterceptorFailClosedIntegrationTest {

  MockMvc mockMvc;

  @Test
  void shouldBlockAccess_whenFlagIsUndefinedInConfig_andDefaultEnabledIsFalse() throws Exception {
    mockMvc
        .perform(get("/undefined-flag-endpoint"))
        .andExpect(status().isForbidden())
        .andExpect(
            content()
                .json(
                    """
                    {
                      "detail"   : "Feature 'undefined-in-config-flag' is not available",
                      "instance" : "/undefined-flag-endpoint",
                      "status"   : 403,
                      "title"    : "Feature flag access denied",
                      "type"     : "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"
                    }
                    """));
  }

  @Autowired
  FeatureFlagInterceptorFailClosedIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
