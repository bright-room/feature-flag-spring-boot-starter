package net.brightroom.featureflag.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when a feature flag's enabled state is changed at runtime.
 *
 * <p>Listeners can subscribe to this event via {@code @EventListener} to react to flag state
 * changes (e.g., clearing caches, logging audit trails, or updating dependent systems).
 */
public class FeatureFlagChangedEvent extends ApplicationEvent {

  private final String featureName;
  private final boolean enabled;

  /**
   * Constructs a {@code FeatureFlagChangedEvent}.
   *
   * @param source the object that published the event
   * @param featureName the name of the feature flag that was changed
   * @param enabled the new enabled state of the feature flag
   */
  public FeatureFlagChangedEvent(Object source, String featureName, boolean enabled) {
    super(source);
    this.featureName = featureName;
    this.enabled = enabled;
  }

  /**
   * Returns the name of the feature flag that was changed.
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
