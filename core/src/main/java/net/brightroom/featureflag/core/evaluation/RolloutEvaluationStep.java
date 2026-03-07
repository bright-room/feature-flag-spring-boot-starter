package net.brightroom.featureflag.core.evaluation;

import java.util.Optional;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import net.brightroom.featureflag.core.rollout.RolloutStrategy;
import org.springframework.core.annotation.Order;

/** Evaluation step that checks whether the request is within the rollout bucket. */
@Order(400)
public class RolloutEvaluationStep implements EvaluationStep {

  private final RolloutStrategy rolloutStrategy;

  /**
   * Creates a new {@code RolloutEvaluationStep}.
   *
   * @param rolloutStrategy the strategy used to determine rollout bucket membership
   */
  public RolloutEvaluationStep(RolloutStrategy rolloutStrategy) {
    this.rolloutStrategy = rolloutStrategy;
  }

  @Override
  public Optional<AccessDecision> evaluate(EvaluationContext context) {
    if (context.rolloutPercentage() >= 100) {
      return Optional.empty();
    }
    FeatureFlagContext flagContext = context.flagContextSupplier().get();
    if (flagContext == null) {
      return Optional.empty(); // fail-open: no context available
    }
    if (!rolloutStrategy.isInRollout(
        context.featureName(), flagContext, context.rolloutPercentage())) {
      return Optional.of(
          AccessDecision.denied(context.featureName(), DeniedReason.ROLLOUT_EXCLUDED));
    }
    return Optional.empty();
  }
}
