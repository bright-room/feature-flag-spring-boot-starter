package net.brightroom.featureflag.webflux.rollout;

/**
 * Default reactive rollout strategy that delegates to the synchronous {@code
 * DefaultRolloutStrategy}.
 *
 * @deprecated Use {@link net.brightroom.featureflag.core.rollout.DefaultReactiveRolloutStrategy}
 *     instead. This alias will be removed in a future release.
 */
@Deprecated
public class DefaultReactiveRolloutStrategy
    extends net.brightroom.featureflag.core.rollout.DefaultReactiveRolloutStrategy
    implements ReactiveRolloutStrategy {

  /** Creates a new {@code DefaultReactiveRolloutStrategy}. */
  public DefaultReactiveRolloutStrategy() {}
}
