package net.brightroom.featureflag.core.evaluation;

import net.brightroom.featureflag.core.condition.ReactiveFeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

/** Reactive evaluation step that checks the SpEL condition expression. */
@Order(300)
public class ReactiveConditionEvaluationStep implements ReactiveEvaluationStep {

  private final ReactiveFeatureFlagConditionEvaluator conditionEvaluator;

  /**
   * Creates a new {@code ReactiveConditionEvaluationStep}.
   *
   * @param conditionEvaluator the reactive evaluator used to evaluate SpEL condition expressions
   */
  public ReactiveConditionEvaluationStep(ReactiveFeatureFlagConditionEvaluator conditionEvaluator) {
    this.conditionEvaluator = conditionEvaluator;
  }

  @Override
  public Mono<AccessDecision> evaluate(EvaluationContext context) {
    if (context.condition().isEmpty()) {
      return Mono.just(AccessDecision.allowed());
    }
    return conditionEvaluator
        .evaluate(context.condition(), context.variables())
        .map(
            passed ->
                passed
                    ? AccessDecision.allowed()
                    : AccessDecision.denied(context.featureName(), DeniedReason.CONDITION_NOT_MET));
  }
}
