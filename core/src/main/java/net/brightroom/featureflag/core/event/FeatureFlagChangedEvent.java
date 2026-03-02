package net.brightroom.featureflag.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when a feature flag's enabled state is changed at runtime.
 *
 * <p>This event is fired by {@link
 * net.brightroom.featureflag.core.provider.MutableFeatureFlagProvider} implementations whenever
 * {@code enable()} or {@code disable()} is invoked. Listeners can subscribe to this event to react
 * to feature flag changes — for example, to invalidate caches or emit metrics.
 *
 * <p>Example listener:
 *
 * <pre>{@code
 * @EventListener
 * public void onFeatureFlagChanged(FeatureFlagChangedEvent event) {
 *     log.info("Feature '{}' changed to {}", event.featureName(), event.enabled());
 * }
 * }</pre>
 */
public class FeatureFlagChangedEvent extends ApplicationEvent {

  private final String featureName;
  private final boolean enabled;

  /**
   * Constructs a new {@code FeatureFlagChangedEvent}.
   *
   * @param source the object that published the event (typically the provider instance)
   * @param featureName the name of the feature whose state changed
   * @param enabled {@code true} if the feature was enabled, {@code false} if disabled
   */
  public FeatureFlagChangedEvent(Object source, String featureName, boolean enabled) {
    super(source);
    this.featureName = featureName;
    this.enabled = enabled;
  }

  /**
   * Returns the name of the feature flag that changed.
   *
   * @return the feature flag name
   */
  public String featureName() {
    return featureName;
  }

  /**
   * Returns the new enabled state of the feature flag.
   *
   * @return {@code true} if the feature was enabled, {@code false} if disabled
   */
  public boolean enabled() {
    return enabled;
  }
}
