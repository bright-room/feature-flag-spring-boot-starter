package net.brightroom.featureflag.core.properties;

/**
 * Configuration for a single feature flag, including its enabled status and rollout percentage.
 *
 * <p>Used as the value type for {@code feature-flags.features} in {@link FeatureFlagProperties}.
 *
 * <p>Configuration example in {@code application.yml}:
 *
 * <pre>{@code
 * feature-flags:
 *   features:
 *     new-feature:
 *       enabled: true
 *       rollout: 50
 *     simple-feature:
 *       enabled: true
 * }</pre>
 */
public class FeatureConfiguration {

  private boolean enabled = true;
  private int rollout = 100;

  /**
   * Returns whether this feature is enabled.
   *
   * @return {@code true} if the feature is enabled, {@code false} otherwise
   */
  public boolean enabled() {
    return enabled;
  }

  /**
   * Returns the rollout percentage for this feature (0–100).
   *
   * <p>100 means fully enabled (all requests). 0 means effectively disabled (no requests even if
   * the flag is enabled).
   *
   * @return the rollout percentage
   */
  public int rollout() {
    return rollout;
  }

  // for property binding
  void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  // for property binding
  void setRollout(int rollout) {
    if (rollout < 0 || rollout > 100) {
      throw new IllegalArgumentException("rollout must be between 0 and 100, but was: " + rollout);
    }
    this.rollout = rollout;
  }

  FeatureConfiguration() {}
}
