package net.brightroom.featureflag.core.provider;

import java.util.Map;

/**
 * An extension of {@link FeatureFlagProvider} that supports runtime modification of feature flag
 * states.
 *
 * <p>Implementations of this interface allow feature flags to be enabled or disabled at runtime
 * without requiring an application restart. This is particularly useful for integration with
 * management endpoints (such as Spring Boot Actuator) or custom administration UIs.
 *
 * <p>Implementations must be thread-safe, as {@link #enable(String)} and {@link #disable(String)}
 * may be called concurrently from multiple threads.
 */
public interface MutableFeatureFlagProvider extends FeatureFlagProvider {

  /**
   * Returns a snapshot of all currently known feature flags and their enabled states.
   *
   * <p>The returned map reflects the state at the time of the call. Subsequent calls to {@link
   * #enable(String)} or {@link #disable(String)} are not reflected in a previously returned map.
   *
   * @return an unmodifiable map of feature flag names to their enabled states
   */
  Map<String, Boolean> getAllFeatures();

  /**
   * Enables the specified feature flag at runtime.
   *
   * <p>After this call, {@link #isFeatureEnabled(String)} returns {@code true} for the given
   * feature name.
   *
   * @param featureName the name of the feature to enable
   */
  void enable(String featureName);

  /**
   * Disables the specified feature flag at runtime.
   *
   * <p>After this call, {@link #isFeatureEnabled(String)} returns {@code false} for the given
   * feature name.
   *
   * @param featureName the name of the feature to disable
   */
  void disable(String featureName);
}
