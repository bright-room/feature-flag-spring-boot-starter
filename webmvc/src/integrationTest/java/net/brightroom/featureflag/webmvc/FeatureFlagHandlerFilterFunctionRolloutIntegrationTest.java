package net.brightroom.featureflag.webmvc;

import static org.springframework.web.servlet.function.RouterFunctions.route;

import java.util.Optional;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.rollout.DefaultRolloutStrategy;
import net.brightroom.featureflag.webmvc.configuration.FeatureFlagMvcTestAutoConfiguration;
import net.brightroom.featureflag.webmvc.context.FeatureFlagContextResolver;
import net.brightroom.featureflag.webmvc.filter.FeatureFlagHandlerFilterFunction;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Verifies rollout behavior for Functional Endpoints through the full MVC stack:
 *
 * <ul>
 *   <li>Custom {@link FeatureFlagContextResolver} bean is respected via
 *       {@code @ConditionalOnMissingBean}, enabling sticky rollout.
 *   <li>Rollout decision is deterministic for a fixed user identifier.
 *   <li>{@link FeatureFlagHandlerFilterFunction#of(String, int)} correctly applies rollout control
 *       via {@code ServerRequest.servletRequest()} in the MVC pipeline.
 * </ul>
 */
@WebMvcTest(properties = {"feature-flags.feature-names.rollout-feature=true"})
@Import({
  FeatureFlagMvcTestAutoConfiguration.class,
  FeatureFlagHandlerFilterFunctionRolloutIntegrationTest.FixedContextResolverConfig.class,
  FeatureFlagHandlerFilterFunctionRolloutIntegrationTest.RolloutRouteConfig.class
})
class FeatureFlagHandlerFilterFunctionRolloutIntegrationTest {

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

  @TestConfiguration
  static class RolloutRouteConfig {
    @Bean
    RouterFunction<ServerResponse> functionalRolloutTestRoute(
        FeatureFlagHandlerFilterFunction featureFlagFilter) {
      return route()
          .GET("/functional/rollout-test", req -> ServerResponse.ok().body("Allowed"))
          .filter(featureFlagFilter.of("rollout-feature", 50))
          .build();
    }
  }

  MockMvc mockMvc;

  @Autowired
  FeatureFlagHandlerFilterFunctionRolloutIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void rollout_returnsDeterministicResult_forFixedUserId() throws Exception {
    ResultMatcher expected =
        IN_ROLLOUT_50
            ? MockMvcResultMatchers.status().isOk()
            : MockMvcResultMatchers.status().isForbidden();
    mockMvc.perform(MockMvcRequestBuilders.get("/functional/rollout-test")).andExpect(expected);
  }

  @Test
  void rollout_sameUserAlwaysGetsSameResult() throws Exception {
    // Call twice — result must be identical (deterministic hashing)
    ResultMatcher expected =
        IN_ROLLOUT_50
            ? MockMvcResultMatchers.status().isOk()
            : MockMvcResultMatchers.status().isForbidden();
    mockMvc.perform(MockMvcRequestBuilders.get("/functional/rollout-test")).andExpect(expected);
    mockMvc.perform(MockMvcRequestBuilders.get("/functional/rollout-test")).andExpect(expected);
  }

  @Test
  void rollout_returnsBody_whenInRollout() throws Exception {
    Assumptions.assumeTrue(IN_ROLLOUT_50, "User not in rollout bucket");
    mockMvc
        .perform(MockMvcRequestBuilders.get("/functional/rollout-test"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().string("Allowed"));
  }
}
