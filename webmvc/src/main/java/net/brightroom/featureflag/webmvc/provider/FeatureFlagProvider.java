package net.brightroom.featureflag.webmvc.provider;

/**
 * Provides a mechanism to check the status of feature flags within an application.
 *
 * <p>The {@code FeatureFlagProvider} interface allows implementations to define how feature flags
 * are stored and accessed, enabling a consistent method for determining whether a specific feature
 * is enabled or disabled at runtime. This can be used to control feature rollout, perform
 * experiments, or toggle functionality dynamically.
 */
public interface FeatureFlagProvider {

  /**
   * Determines whether a specific feature is enabled based on its feature flag.
   *
   * @param featureName the name of the feature whose status is to be verified
   * @return {@code true} if the feature is enabled, {@code false} otherwise
   */
  boolean isFeatureEnabled(String featureName);
}
