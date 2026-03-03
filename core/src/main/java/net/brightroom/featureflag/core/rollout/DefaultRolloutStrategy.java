package net.brightroom.featureflag.core.rollout;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.brightroom.featureflag.core.context.FeatureFlagContext;

/**
 * Default rollout strategy using SHA-256 hash bucketing.
 *
 * <p>Computes a bucket number (0–99) from the SHA-256 hash of {@code featureName:userIdentifier}.
 * The bucket is compared with the rollout percentage to determine inclusion. The same input always
 * produces the same bucket (deterministic), so sticky rollout is achieved when the caller provides
 * a stable {@code userIdentifier}.
 */
public class DefaultRolloutStrategy implements RolloutStrategy {

  @Override
  public boolean isInRollout(String featureName, FeatureFlagContext context, int percentage) {
    if (percentage <= 0) return false;
    if (percentage >= 100) return true;
    int bucket = calculateBucket(featureName, context.userIdentifier());
    return bucket < percentage;
  }

  private int calculateBucket(String featureName, String userIdentifier) {
    String key = featureName + ":" + userIdentifier;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(key.getBytes(StandardCharsets.UTF_8));
      int value =
          ((hash[0] & 0xFF) << 24)
              | ((hash[1] & 0xFF) << 16)
              | ((hash[2] & 0xFF) << 8)
              | (hash[3] & 0xFF);
      return Integer.remainderUnsigned(value, 100);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
