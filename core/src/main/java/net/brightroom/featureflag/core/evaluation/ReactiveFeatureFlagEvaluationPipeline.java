package net.brightroom.featureflag.core.evaluation;

import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive feature flag evaluation pipeline.
 *
 * <p>Executes a sequence of {@link ReactiveEvaluationStep}s sequentially. Returns the first {@link
 * AccessDecision.Denied} encountered, or {@link AccessDecision.Allowed} if all steps pass.
 *
 * <p>The steps list is expected to be sorted by {@link org.springframework.core.annotation.Order},
 * which Spring handles automatically when injecting {@code List<ReactiveEvaluationStep>}.
 */
public class ReactiveFeatureFlagEvaluationPipeline {

  private final List<ReactiveEvaluationStep> steps;

  /**
   * Creates a new {@code ReactiveFeatureFlagEvaluationPipeline}.
   *
   * @param steps the evaluation steps to execute in order; must not be null
   */
  public ReactiveFeatureFlagEvaluationPipeline(List<ReactiveEvaluationStep> steps) {
    this.steps = steps;
  }

  /**
   * Evaluates all steps sequentially and returns the first denied decision or {@link
   * AccessDecision#allowed()} if all steps pass.
   *
   * <p>Short-circuits on the first {@link AccessDecision.Denied} result.
   *
   * @param context the evaluation context
   * @return a {@link Mono} emitting the access decision
   */
  public Mono<AccessDecision> evaluate(EvaluationContext context) {
    return Flux.fromIterable(steps)
        .concatMap(step -> step.evaluate(context))
        .filter(AccessDecision.Denied.class::isInstance)
        .next()
        .defaultIfEmpty(AccessDecision.allowed());
  }
}
