package net.brightroom.featureflag.webflux.provider;

import reactor.core.publisher.Mono;

/**
 * Provides a reactive mechanism to check the status of feature flags within an application.
 *
 * <p>The {@code ReactiveFeatureFlagProvider} interface allows implementations to define how feature
 * flags are stored and accessed in a non-blocking manner, enabling a consistent method for
 * determining whether a specific feature is enabled or disabled at runtime. This can be used to
 * control feature rollout, perform experiments, or toggle functionality dynamically.
 *
 * <p><b>Undefined flag policy:</b> Implementations must decide what to return when {@code
 * featureName} is not known to the provider. The built-in {@link
 * InMemoryReactiveFeatureFlagProvider} uses a <em>fail-closed</em> policy by default (returns
 * {@code false} for unknown flags), which can be changed to fail-open via {@code
 * feature-flags.default-enabled: true}. Custom implementations should document their own policy
 * clearly.
 */
public interface ReactiveFeatureFlagProvider {

  /**
   * Determines whether a specific feature is enabled based on its feature flag.
   *
   * <p>The return value for a feature name that is not managed by this provider is
   * implementation-defined. See the implementing class for its undefined-flag policy.
   *
   * @param featureName the name of the feature whose status is to be verified
   * @return a {@link Mono} emitting {@code true} if the feature is enabled, {@code false} otherwise
   */
  Mono<Boolean> isFeatureEnabled(String featureName);
}
