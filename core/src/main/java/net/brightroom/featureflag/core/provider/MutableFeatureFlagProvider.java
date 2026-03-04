package net.brightroom.featureflag.core.provider;

import java.util.Map;

/**
 * An extension of {@link FeatureFlagProvider} that supports dynamic mutation of feature flags at
 * runtime.
 *
 * <p>Implementations must be thread-safe, as flags may be read and updated concurrently.
 *
 * <p>This interface serves as an SPI for external storage backends (e.g., Redis, databases) that
 * need to expose mutable feature flag state without depending on the {@code actuator} module.
 */
public interface MutableFeatureFlagProvider extends FeatureFlagProvider {

  /**
   * Returns a snapshot of all currently configured feature flags and their enabled states.
   *
   * <p>The returned map must be an immutable copy; mutations to the returned map must not affect
   * the provider's internal state.
   *
   * @return an immutable map of feature flag names to their enabled states
   */
  Map<String, Boolean> getFeatures();

  /**
   * Updates the enabled state of the specified feature flag.
   *
   * <p>If the feature flag does not exist, it must be created with the given state.
   *
   * <p><b>Note:</b> This method does not publish {@code FeatureFlagChangedEvent}. Event publishing
   * is handled by the actuator endpoint ({@code FeatureFlagEndpoint}). If you call this method
   * directly and need event notification, publish the event manually via {@code
   * ApplicationEventPublisher}.
   *
   * @param featureName the name of the feature flag to update
   * @param enabled {@code true} to enable the feature, {@code false} to disable it
   */
  void setFeatureEnabled(String featureName, boolean enabled);

  /**
   * Removes the specified feature flag from this provider.
   *
   * <p>After removal, {@link #isFeatureEnabled(String)} for this flag will return the default
   * enabled value. If the flag does not exist, this method is a no-op and returns {@code false}.
   *
   * <p><b>Note:</b> This method does not publish {@code FeatureFlagRemovedEvent}. Event publishing
   * is handled by the actuator endpoint ({@code FeatureFlagEndpoint}). If you call this method
   * directly and need event notification, publish the event manually via {@code
   * ApplicationEventPublisher}.
   *
   * @param featureName the name of the feature flag to remove
   * @return {@code true} if the flag existed and was removed, {@code false} if it did not exist
   */
  boolean removeFeature(String featureName);
}
