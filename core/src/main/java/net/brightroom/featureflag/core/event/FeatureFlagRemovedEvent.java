package net.brightroom.featureflag.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when a feature flag is removed at runtime.
 *
 * <p>Listeners can subscribe to this event via {@code @EventListener} to react to flag removal
 * (e.g., purging caches, logging audit trails, or updating dependent systems).
 *
 * <p><b>Reactive environments:</b> This event is published synchronously via {@link
 * org.springframework.context.ApplicationEventPublisher} from the actuator endpoint's management
 * thread. In WebFlux applications, listeners should avoid blocking the calling thread. If
 * long-running or reactive work is needed in response to flag removal, offload it to a separate
 * scheduler (e.g., {@code Schedulers.boundedElastic()}) or publish a message to a reactive stream.
 */
public class FeatureFlagRemovedEvent extends ApplicationEvent {

  private final String featureName;

  /**
   * Constructs a {@code FeatureFlagRemovedEvent}.
   *
   * @param source the object that published the event
   * @param featureName the name of the feature flag that was removed
   */
  public FeatureFlagRemovedEvent(Object source, String featureName) {
    super(source);
    this.featureName = featureName;
  }

  /**
   * Returns the name of the feature flag that was removed.
   *
   * @return the feature flag name
   */
  public String featureName() {
    return featureName;
  }
}
