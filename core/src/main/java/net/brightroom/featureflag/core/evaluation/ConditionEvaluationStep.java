package net.brightroom.featureflag.core.evaluation;

import java.util.Optional;
import net.brightroom.featureflag.core.condition.FeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import org.springframework.core.annotation.Order;

/** Evaluation step that checks the SpEL condition expression. */
@Order(300)
public class ConditionEvaluationStep implements EvaluationStep {

  private final FeatureFlagConditionEvaluator conditionEvaluator;

  /**
   * Creates a new {@code ConditionEvaluationStep}.
   *
   * @param conditionEvaluator the evaluator used to evaluate SpEL condition expressions
   */
  public ConditionEvaluationStep(FeatureFlagConditionEvaluator conditionEvaluator) {
    this.conditionEvaluator = conditionEvaluator;
  }

  @Override
  public Optional<AccessDecision> evaluate(EvaluationContext context) {
    if (context.condition().isEmpty()) {
      return Optional.empty();
    }
    if (!conditionEvaluator.evaluate(context.condition(), context.variables())) {
      return Optional.of(
          AccessDecision.denied(context.featureName(), DeniedReason.CONDITION_NOT_MET));
    }
    return Optional.empty();
  }
}
