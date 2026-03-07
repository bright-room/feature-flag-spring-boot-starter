package net.brightroom.featureflag.webflux.rollout;

/**
 * Reactive strategy for determining whether a given context is within the rollout bucket.
 *
 * @deprecated Use {@link net.brightroom.featureflag.core.rollout.ReactiveRolloutStrategy} instead.
 *     This alias will be removed in a future release.
 */
@Deprecated
public interface ReactiveRolloutStrategy
    extends net.brightroom.featureflag.core.rollout.ReactiveRolloutStrategy {}
