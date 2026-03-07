package net.brightroom.featureflag.webmvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.rollout.DefaultRolloutStrategy;
import net.brightroom.featureflag.webmvc.configuration.FeatureFlagMvcTestAutoConfiguration;
import net.brightroom.featureflag.webmvc.context.FeatureFlagContextResolver;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagClassRolloutController;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagRolloutController;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Verifies rollout behavior through the full MVC stack:
 *
 * <ul>
 *   <li>Custom {@link FeatureFlagContextResolver} bean is respected via
 *       {@code @ConditionalOnMissingBean}, enabling sticky rollout.
 *   <li>Rollout decision is deterministic for a fixed user identifier.
 *   <li>Class-level {@code @FeatureFlag} with {@code rollout} is processed correctly by the
 *       interceptor.
 * </ul>
 */
@WebMvcTest(
    properties = {
      "feature-flags.features.rollout-feature.enabled=true",
      "feature-flags.features.rollout-feature.rollout=50"
    },
    controllers = {FeatureFlagRolloutController.class, FeatureFlagClassRolloutController.class})
@Import({
  FeatureFlagMvcTestAutoConfiguration.class,
  FeatureFlagInterceptorRolloutIntegrationTest.FixedContextResolverConfig.class
})
class FeatureFlagInterceptorRolloutIntegrationTest {

  private static final FeatureFlagContext FIXED_CONTEXT = new FeatureFlagContext("fixed-user-id");
  private static final boolean IN_ROLLOUT_50 =
      new DefaultRolloutStrategy().isInRollout("rollout-feature", FIXED_CONTEXT, 50);

  @TestConfiguration
  static class FixedContextResolverConfig {
    @Bean
    FeatureFlagContextResolver contextResolver() {
      return request -> Optional.of(FIXED_CONTEXT);
    }
  }

  MockMvc mockMvc;

  @Autowired
  FeatureFlagInterceptorRolloutIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void methodLevel_stickyRollout_returnsDeterministicResult_forFixedUserId() throws Exception {
    ResultMatcher expected = IN_ROLLOUT_50 ? status().isOk() : status().isForbidden();
    mockMvc.perform(get("/test/rollout")).andExpect(expected);
  }

  @Test
  void methodLevel_stickyRollout_sameUserAlwaysGetsSameResult() throws Exception {
    // Call twice — result must be identical (deterministic hashing)
    ResultMatcher expected = IN_ROLLOUT_50 ? status().isOk() : status().isForbidden();
    mockMvc.perform(get("/test/rollout")).andExpect(expected);
    mockMvc.perform(get("/test/rollout")).andExpect(expected);
  }

  @Test
  void classLevel_stickyRollout_returnsDeterministicResult_forFixedUserId() throws Exception {
    // Verifies that class-level @FeatureFlag with rollout is processed through the interceptor
    ResultMatcher expected = IN_ROLLOUT_50 ? status().isOk() : status().isForbidden();
    mockMvc.perform(get("/test/class-rollout")).andExpect(expected);
  }

  @Test
  void methodLevel_rolloutAllowed_returnsBody_whenInRollout() throws Exception {
    // When the fixed user is in rollout, the response body should be "Allowed"
    Assumptions.assumeTrue(IN_ROLLOUT_50, "User not in rollout bucket");
    mockMvc
        .perform(get("/test/rollout"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }
}
