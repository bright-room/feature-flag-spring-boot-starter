package net.brightroom.featureflag.core.evaluation;

import java.util.Optional;

/**
 * SPI for a single step in the synchronous feature flag evaluation pipeline.
 *
 * <p>Implement this interface and register the implementation as a Spring {@code @Bean} to add a
 * custom evaluation step. Use {@link org.springframework.core.annotation.Order} to control the
 * position of the step in the pipeline. The default steps use order values 100, 200, 300, and 400,
 * so custom steps can be inserted between them (e.g., {@code @Order(150)}).
 */
public interface EvaluationStep {

  /**
   * Evaluates one step of the feature flag decision pipeline.
   *
   * @param context the evaluation context containing all inputs for this step
   * @return {@link Optional#empty()} if this step passes (proceed to the next step), or an {@link
   *     Optional} containing an {@link AccessDecision.Denied} if this step rejects the request
   */
  Optional<AccessDecision> evaluate(EvaluationContext context);
}
