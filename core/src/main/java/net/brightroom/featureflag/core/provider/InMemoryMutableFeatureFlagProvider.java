package net.brightroom.featureflag.core.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * A thread-safe, in-memory implementation of {@link MutableFeatureFlagProvider} that supports
 * runtime modification of feature flag states.
 *
 * <p>Feature flags are stored in a {@link ConcurrentHashMap}, allowing concurrent reads and writes
 * without external synchronization. Each call to {@link #enable(String)} or {@link
 * #disable(String)} publishes a {@link FeatureFlagChangedEvent} via the provided {@link
 * ApplicationEventPublisher}.
 *
 * <p>This implementation is registered automatically when the {@code actuator} module is on the
 * classpath and no other {@link FeatureFlagProvider} bean is already registered. It replaces {@link
 * InMemoryFeatureFlagProvider} as the default provider in that scenario.
 */
public class InMemoryMutableFeatureFlagProvider implements MutableFeatureFlagProvider {

  private final ConcurrentHashMap<String, Boolean> features;
  private final boolean defaultEnabled;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructs an instance with the given initial feature flag states.
   *
   * @param features initial feature flag names and their enabled states
   * @param defaultEnabled the default state for feature flags not present in {@code features}. Use
   *     {@code false} for fail-closed behavior or {@code true} for fail-open behavior.
   * @param eventPublisher the publisher used to broadcast {@link FeatureFlagChangedEvent}s
   */
  public InMemoryMutableFeatureFlagProvider(
      Map<String, Boolean> features,
      boolean defaultEnabled,
      ApplicationEventPublisher eventPublisher) {
    this.features = new ConcurrentHashMap<>(features);
    this.defaultEnabled = defaultEnabled;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public boolean isFeatureEnabled(String featureName) {
    return features.getOrDefault(featureName, defaultEnabled);
  }

  @Override
  public Map<String, Boolean> getAllFeatures() {
    return Map.copyOf(features);
  }

  @Override
  public void enable(String featureName) {
    features.put(featureName, true);
    eventPublisher.publishEvent(new FeatureFlagChangedEvent(this, featureName, true));
  }

  @Override
  public void disable(String featureName) {
    features.put(featureName, false);
    eventPublisher.publishEvent(new FeatureFlagChangedEvent(this, featureName, false));
  }
}
