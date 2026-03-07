package net.brightroom.featureflag.core.evaluation;

import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

/** Reactive evaluation step that checks whether the feature flag is enabled. */
@Order(100)
public class ReactiveEnabledEvaluationStep implements ReactiveEvaluationStep {

  private final ReactiveFeatureFlagProvider provider;

  /**
   * Creates a new {@code ReactiveEnabledEvaluationStep}.
   *
   * @param provider the provider used to check whether a feature flag is enabled
   */
  public ReactiveEnabledEvaluationStep(ReactiveFeatureFlagProvider provider) {
    this.provider = provider;
  }

  @Override
  public Mono<AccessDecision> evaluate(EvaluationContext context) {
    return provider
        .isFeatureEnabled(context.featureName())
        .defaultIfEmpty(false)
        .map(
            enabled ->
                enabled
                    ? AccessDecision.allowed()
                    : AccessDecision.denied(context.featureName(), DeniedReason.DISABLED));
  }
}
