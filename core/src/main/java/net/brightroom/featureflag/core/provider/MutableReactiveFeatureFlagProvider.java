package net.brightroom.featureflag.core.provider;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * A reactive extension of {@link ReactiveFeatureFlagProvider} that supports dynamic mutation of
 * feature flags at runtime.
 *
 * <p>Implementations must be thread-safe, as flags may be read and updated concurrently.
 *
 * <p>This interface serves as an SPI for external storage backends (e.g., Redis, databases) that
 * need to expose mutable feature flag state in a reactive manner without depending on the {@code
 * actuator} module.
 */
public interface MutableReactiveFeatureFlagProvider extends ReactiveFeatureFlagProvider {

  /**
   * Returns a snapshot of all currently configured feature flags and their enabled states.
   *
   * <p>The returned map must be an immutable copy; mutations to the returned map must not affect
   * the provider's internal state.
   *
   * @return a {@link Mono} emitting an immutable map of feature flag names to their enabled states
   */
  Mono<Map<String, Boolean>> getFeatures();

  /**
   * Updates the enabled state of the specified feature flag.
   *
   * <p>If the feature flag does not exist, it must be created with the given state.
   *
   * <p><b>Note:</b> This method does not publish {@code FeatureFlagChangedEvent}. Event publishing
   * is handled by the actuator endpoint ({@code ReactiveFeatureFlagEndpoint}). If you call this
   * method directly and need event notification, publish the event manually via {@code
   * ApplicationEventPublisher}.
   *
   * @param featureName the name of the feature flag to update
   * @param enabled {@code true} to enable the feature, {@code false} to disable it
   * @return a {@link Mono} that completes when the update is applied
   */
  Mono<Void> setFeatureEnabled(String featureName, boolean enabled);
}
