package net.brightroom.featureflag.core.evaluation;

import java.util.Optional;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import org.springframework.core.annotation.Order;

/** Evaluation step that checks whether the feature flag is enabled. */
@Order(100)
public class EnabledEvaluationStep implements EvaluationStep {

  private final FeatureFlagProvider provider;

  /**
   * Creates a new {@code EnabledEvaluationStep}.
   *
   * @param provider the provider used to check whether a feature flag is enabled
   */
  public EnabledEvaluationStep(FeatureFlagProvider provider) {
    this.provider = provider;
  }

  @Override
  public Optional<AccessDecision> evaluate(EvaluationContext context) {
    if (!provider.isFeatureEnabled(context.featureName())) {
      return Optional.of(AccessDecision.denied(context.featureName(), DeniedReason.DISABLED));
    }
    return Optional.empty();
  }
}
