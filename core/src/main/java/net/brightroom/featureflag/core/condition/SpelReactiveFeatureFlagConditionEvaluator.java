package net.brightroom.featureflag.core.condition;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Default {@link ReactiveFeatureFlagConditionEvaluator} that wraps a synchronous {@link
 * FeatureFlagConditionEvaluator} via {@link Mono#fromCallable}.
 *
 * <p>This implementation is CPU-bound and runs on the subscribing thread. It is suitable for the
 * default SpEL-based evaluation which is fast. Custom implementations that need to perform
 * non-blocking I/O should implement {@link ReactiveFeatureFlagConditionEvaluator} directly and
 * subscribe on an appropriate scheduler.
 */
public class SpelReactiveFeatureFlagConditionEvaluator
    implements ReactiveFeatureFlagConditionEvaluator {

  private final FeatureFlagConditionEvaluator delegate;

  /**
   * Creates a new {@code SpelReactiveFeatureFlagConditionEvaluator}.
   *
   * @param delegate the synchronous evaluator to delegate to; must not be null
   */
  public SpelReactiveFeatureFlagConditionEvaluator(FeatureFlagConditionEvaluator delegate) {
    this.delegate = delegate;
  }

  @Override
  public Mono<Boolean> evaluate(String expression, Map<String, Object> variables) {
    return Mono.fromCallable(() -> delegate.evaluate(expression, variables));
  }
}
