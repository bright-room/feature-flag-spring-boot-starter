package net.brightroom.featureflag.core.condition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.expression.MapAccessor;
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
 * <p>Uses {@link SimpleEvaluationContext} with {@link DataBindingPropertyAccessor} (read-only) and
 * {@link MapAccessor} to safely evaluate expressions. Type references ({@code T(...)}),
 * constructors ({@code new ...}), and bean references ({@code @beanName}) are structurally
 * excluded.
 *
 * <p>Parsed expressions are cached in a {@link ConcurrentHashMap} for performance, since condition
 * expressions are static (annotation-derived).
 */
public class SpelFeatureFlagConditionEvaluator implements FeatureFlagConditionEvaluator {

  private static final Log log = LogFactory.getLog(SpelFeatureFlagConditionEvaluator.class);

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
  public boolean evaluate(String expression, Map<String, Object> variables) {
    try {
      Expression expr = cache.computeIfAbsent(expression, parser::parseExpression);
      SimpleEvaluationContext context =
          SimpleEvaluationContext.forPropertyAccessors(
                  DataBindingPropertyAccessor.forReadOnlyAccess(), new MapAccessor())
              .withRootObject(variables)
              .build();
      Boolean result = expr.getValue(context, Boolean.class);
      return Boolean.TRUE.equals(result);
    } catch (EvaluationException | ParseException e) {
      log.warn(
          "Failed to evaluate feature flag condition expression '"
              + expression
              + "': "
              + e.getMessage());
      return !failOnError;
    }
  }
}
