package net.brightroom.featureflag.core.condition;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.DataBindingPropertyAccessor;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

/**
 * Default {@link FeatureFlagConditionEvaluator} implementation using Spring Expression Language
 * (SpEL).
 *
 * <p>Uses {@link SimpleEvaluationContext} with {@link DataBindingPropertyAccessor} (read-only) to
 * safely evaluate expressions against a typed {@link ConditionVariables} root object. Type
 * references ({@code T(...)}), constructors ({@code new ...}), and bean references ({@code
 * @beanName}) are structurally excluded.
 *
 * <p>Parsed expressions are cached in a {@link ConcurrentHashMap} for performance, since condition
 * expressions are static (annotation-derived).
 */
public class SpelFeatureFlagConditionEvaluator implements FeatureFlagConditionEvaluator {

  private static final Log log = LogFactory.getLog(SpelFeatureFlagConditionEvaluator.class);
  private static final DataBindingPropertyAccessor READ_ONLY_ACCESSOR =
      DataBindingPropertyAccessor.forReadOnlyAccess();

  private final SpelExpressionParser parser = new SpelExpressionParser();
  private final ConcurrentMap<String, Expression> cache = new ConcurrentHashMap<>();
  private final boolean failOnError;

  /**
   * Creates a new {@code SpelFeatureFlagConditionEvaluator}.
   *
   * @param failOnError when {@code true}, evaluation errors result in {@code false} (fail-closed);
   *     when {@code false}, errors result in {@code true} (fail-open)
   */
  public SpelFeatureFlagConditionEvaluator(boolean failOnError) {
    this.failOnError = failOnError;
  }

  @Override
  public boolean evaluate(String expression, ConditionVariables variables) {
    try {
      Expression expr = cache.computeIfAbsent(expression, parser::parseExpression);
      SimpleEvaluationContext context =
          SimpleEvaluationContext.forPropertyAccessors(READ_ONLY_ACCESSOR)
              .withRootObject(variables)
              .build();
      Boolean result = expr.getValue(context, Boolean.class);
      return Boolean.TRUE.equals(result);
    } catch (EvaluationException | ParseException e) {
      log.warn(
          String.format(
              "Failed to evaluate feature flag condition expression '%s': %s",
              expression, e.getMessage()));
      return !failOnError;
    }
  }
}
