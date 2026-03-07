package net.brightroom.featureflag.core.evaluation;

import java.util.List;
import java.util.Optional;

/**
 * Synchronous feature flag evaluation pipeline.
 *
 * <p>Executes a sequence of {@link EvaluationStep}s in order. Returns the first {@link
 * AccessDecision.Denied} encountered, or {@link AccessDecision.Allowed} if all steps pass.
 *
 * <p>The steps list is expected to be sorted by {@link org.springframework.core.annotation.Order},
 * which Spring handles automatically when injecting {@code List<EvaluationStep>}.
 */
public class FeatureFlagEvaluationPipeline {

  private final List<EvaluationStep> steps;

  /**
   * Creates a new {@code FeatureFlagEvaluationPipeline}.
   *
   * @param steps the evaluation steps to execute in order; must not be null
   */
  public FeatureFlagEvaluationPipeline(List<EvaluationStep> steps) {
    this.steps = steps;
  }

  /**
   * Evaluates all steps in order and returns the first denied decision or {@link
   * AccessDecision#allowed()} if all steps pass.
   *
   * @param context the evaluation context
   * @return the access decision
   */
  public AccessDecision evaluate(EvaluationContext context) {
    for (EvaluationStep step : steps) {
      Optional<AccessDecision> result = step.evaluate(context);
      if (result.isPresent()) {
        return result.get();
      }
    }
    return AccessDecision.allowed();
  }
}
