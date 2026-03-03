package net.brightroom.featureflag.webflux.rollout;

import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.rollout.DefaultRolloutStrategy;
import reactor.core.publisher.Mono;

/**
 * Default {@link ReactiveRolloutStrategy} that delegates to {@link DefaultRolloutStrategy}.
 *
 * <p>Wraps the synchronous SHA-256 hash bucketing logic in a non-blocking {@link Mono}.
 */
public class DefaultReactiveRolloutStrategy implements ReactiveRolloutStrategy {

  private final DefaultRolloutStrategy delegate = new DefaultRolloutStrategy();

  @Override
  public Mono<Boolean> isInRollout(String featureName, FeatureFlagContext context, int percentage) {
    return Mono.fromCallable(() -> delegate.isInRollout(featureName, context, percentage));
  }
}
