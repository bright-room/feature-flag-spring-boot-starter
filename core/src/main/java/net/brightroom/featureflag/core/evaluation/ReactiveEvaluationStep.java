package net.brightroom.featureflag.core.evaluation;

import reactor.core.publisher.Mono;

/**
 * SPI for a single step in the reactive feature flag evaluation pipeline.
 *
 * <p>Implement this interface and register the implementation as a Spring {@code @Bean} to add a
 * custom reactive evaluation step. Use {@link org.springframework.core.annotation.Order} to control
 * the position of the step in the pipeline. The default steps use order values 100, 200, 300, and
 * 400, so custom steps can be inserted between them (e.g., {@code @Order(150)}).
 */
public interface ReactiveEvaluationStep {

  /**
   * Evaluates one step of the reactive feature flag decision pipeline.
   *
   * @param context the evaluation context containing all inputs for this step
   * @return a {@link Mono} emitting {@link AccessDecision.Allowed} if this step passes, or {@link
   *     AccessDecision.Denied} if this step rejects the request
   */
  Mono<AccessDecision> evaluate(EvaluationContext context);
}
