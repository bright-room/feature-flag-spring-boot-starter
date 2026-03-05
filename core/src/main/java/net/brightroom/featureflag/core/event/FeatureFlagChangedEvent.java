package net.brightroom.featureflag.core.event;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a feature flag's enabled state or rollout percentage is changed at runtime.
 *
 * <p>Listeners can subscribe to this event via {@code @EventListener} to react to flag state
 * changes (e.g., clearing caches, logging audit trails, or updating dependent systems).
 *
 * <p><b>Reactive environments:</b> This event is published synchronously via {@link
 * org.springframework.context.ApplicationEventPublisher} from the actuator endpoint's management
 * thread. In WebFlux applications, listeners should avoid blocking the calling thread. If
 * long-running or reactive work is needed in response to flag changes, offload it to a separate
 * scheduler (e.g., {@code Schedulers.boundedElastic()}) or publish a message to a reactive stream.
 */
public class FeatureFlagChangedEvent extends ApplicationEvent {

  /** The name of the feature flag that was changed. */
  private final String featureName;

  /** Whether the feature flag is enabled after this change. */
  private final boolean enabled;

  /** The new rollout percentage, or {@code null} if the rollout was not changed. */
  @Nullable private final Integer rolloutPercentage;

  /**
   * Constructs a {@code FeatureFlagChangedEvent} when only the enabled state changed.
   *
   * @param source the object that published the event
   * @param featureName the name of the feature flag that was changed
   * @param enabled the new enabled state of the feature flag
   */
  public FeatureFlagChangedEvent(Object source, String featureName, boolean enabled) {
    this(source, featureName, enabled, null);
  }

  /**
   * Constructs a {@code FeatureFlagChangedEvent} with an optional rollout percentage change.
   *
   * @param source the object that published the event
   * @param featureName the name of the feature flag that was changed
   * @param enabled the new enabled state of the feature flag
   * @param rolloutPercentage the new rollout percentage, or {@code null} if the rollout was not
   *     changed
   */
  public FeatureFlagChangedEvent(
      Object source, String featureName, boolean enabled, @Nullable Integer rolloutPercentage) {
    super(source);
    this.featureName = featureName;
    this.enabled = enabled;
    this.rolloutPercentage = rolloutPercentage;
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

  /**
   * Returns the new rollout percentage, or {@code null} if the rollout was not changed.
   *
   * @return the rollout percentage (0–100), or {@code null}
   */
  @Nullable
  public Integer rolloutPercentage() {
    return rolloutPercentage;
  }
}
