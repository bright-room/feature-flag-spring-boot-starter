package net.brightroom.featureflag.webmvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import net.brightroom.featureflag.webmvc.configuration.FeatureFlagMvcTestAutoConfiguration;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagRolloutController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies gradual rollout behaviour for {@code @FeatureFlag(rollout = N)}.
 *
 * <p>The feature {@code "rollout-test-feature"} is enabled in {@code application.yaml}. Each
 * endpoint uses a different rollout percentage to exercise the three code paths:
 *
 * <ul>
 *   <li>{@code rollout = 100} — all users allowed
 *   <li>{@code rollout = 0} — all users blocked
 *   <li>{@code rollout = 50} — hash-based bucket assignment
 * </ul>
 */
@WebMvcTest(controllers = FeatureFlagRolloutController.class)
@Import(FeatureFlagMvcTestAutoConfiguration.class)
class FeatureFlagInterceptorRolloutIntegrationTest {

  MockMvc mockMvc;

  @Test
  void shouldAllowAccess_whenRolloutIs100() throws Exception {
    mockMvc.perform(get("/rollout/full")).andExpect(status().isOk());
  }

  @Test
  void shouldBlockAccess_whenRolloutIs0_evenIfFeatureIsEnabled() throws Exception {
    mockMvc.perform(get("/rollout/none")).andExpect(status().isForbidden());
  }

  @Test
  void shouldAllowAccess_forUserInAllowedBucket_whenPartialRollout() throws Exception {
    String featureName = "rollout-test-feature";
    int rollout = 50;
    String userId = findUserIdWithBucketBelow(featureName, rollout);
    mockMvc
        .perform(get("/rollout/partial").session(new MockHttpSession(null, userId)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldBlockAccess_forUserOutsideAllowedBucket_whenPartialRollout() throws Exception {
    String featureName = "rollout-test-feature";
    int rollout = 50;
    String userId = findUserIdWithBucketAtLeast(featureName, rollout);
    mockMvc
        .perform(get("/rollout/partial").session(new MockHttpSession(null, userId)))
        .andExpect(status().isForbidden());
  }

  // ---------------------------------------------------------------------------
  // Helpers — mirror the bucket computation in FeatureFlagInterceptor
  // ---------------------------------------------------------------------------

  private static String findUserIdWithBucketBelow(String featureName, int threshold)
      throws Exception {
    for (int i = 0; i < 1000; i++) {
      String userId = "allowed-user-" + i;
      if (computeBucket(featureName, userId) < threshold) {
        return userId;
      }
    }
    throw new IllegalStateException("Could not find a userId with bucket below " + threshold);
  }

  private static String findUserIdWithBucketAtLeast(String featureName, int threshold)
      throws Exception {
    for (int i = 0; i < 1000; i++) {
      String userId = "blocked-user-" + i;
      if (computeBucket(featureName, userId) >= threshold) {
        return userId;
      }
    }
    throw new IllegalStateException("Could not find a userId with bucket at least " + threshold);
  }

  private static int computeBucket(String featureName, String userId) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] hash = md.digest((featureName + ":" + userId).getBytes(StandardCharsets.UTF_8));
    int value =
        ((hash[0] & 0xFF) << 24)
            | ((hash[1] & 0xFF) << 16)
            | ((hash[2] & 0xFF) << 8)
            | (hash[3] & 0xFF);
    return Integer.remainderUnsigned(value, 100);
  }

  @Autowired
  FeatureFlagInterceptorRolloutIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
