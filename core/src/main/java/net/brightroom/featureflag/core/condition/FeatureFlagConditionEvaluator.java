package net.brightroom.featureflag.core.condition;

/**
 * SPI for evaluating SpEL condition expressions against request context variables.
 *
 * <p>Implementations evaluate a SpEL expression with the given variables and return whether the
 * condition is satisfied. The default implementation uses {@link
 * org.springframework.expression.spel.support.SimpleEvaluationContext} for safe evaluation.
 *
 * <p>Register a custom bean to replace the default implementation:
 *
 * <pre>{@code
 * @Bean
 * FeatureFlagConditionEvaluator customEvaluator() {
 *     return (expression, variables) -> ...;
 * }
 * }</pre>
 */
public interface FeatureFlagConditionEvaluator {

  /**
   * Evaluates a SpEL condition expression against the given variables.
   *
   * @param expression the SpEL expression to evaluate
   * @param variables the variables available in the expression context
   * @return {@code true} if the condition is satisfied
   */
  boolean evaluate(String expression, ConditionVariables variables);
}
