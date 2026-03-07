package net.brightroom.featureflag.core.condition;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Reactive SPI for evaluating SpEL condition expressions against request context variables.
 *
 * <p>Implementations evaluate a SpEL expression with the given variables and return a {@link
 * Mono}{@code <Boolean>} indicating whether the condition is satisfied. The default implementation
 * wraps {@link SpelFeatureFlagConditionEvaluator} via {@link Mono#fromCallable}.
 *
 * <p>Register a custom bean to replace the default implementation:
 *
 * <pre>{@code
 * @Bean
 * ReactiveFeatureFlagConditionEvaluator customEvaluator() {
 *     return (expression, variables) -> ...;
 * }
 * }</pre>
 *
 * <p>Custom implementations that perform non-blocking I/O (e.g., querying a remote condition
 * service) should return a {@link Mono} that executes on an appropriate scheduler and must not
 * block the event loop thread.
 */
public interface ReactiveFeatureFlagConditionEvaluator {

  /**
   * Evaluates a SpEL condition expression against the given variables.
   *
   * @param expression the SpEL expression to evaluate
   * @param variables the variables available in the expression context
   * @return a {@link Mono} emitting {@code true} if the condition is satisfied
   */
  Mono<Boolean> evaluate(String expression, Map<String, Object> variables);
}
