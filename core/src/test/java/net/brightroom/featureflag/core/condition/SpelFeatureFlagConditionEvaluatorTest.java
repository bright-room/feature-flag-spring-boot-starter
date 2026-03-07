package net.brightroom.featureflag.core.condition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpelFeatureFlagConditionEvaluatorTest {

  private final SpelFeatureFlagConditionEvaluator evaluator =
      new SpelFeatureFlagConditionEvaluator(true);

  private Map<String, Object> buildVariables() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("headers", new HashMap<>(Map.of("X-Beta", "true", "X-Region", "us-east-1")));
    variables.put("params", new HashMap<>(Map.of("variant", "B", "debug", "true")));
    variables.put("cookies", new HashMap<>(Map.of("session", "abc123")));
    variables.put("path", "/api/v2/users");
    variables.put("method", "POST");
    variables.put("remoteAddress", "192.168.1.1");
    return variables;
  }

  // --- header checks ---

  @Test
  void evaluate_returnsTrue_whenHeaderExists() {
    assertThat(evaluator.evaluate("headers['X-Beta'] != null", buildVariables())).isTrue();
  }

  @Test
  void evaluate_returnsFalse_whenHeaderDoesNotExist() {
    assertThat(evaluator.evaluate("headers['X-Missing'] != null", buildVariables())).isFalse();
  }

  @Test
  void evaluate_returnsTrue_whenHeaderValueMatches() {
    assertThat(evaluator.evaluate("headers['X-Region'] == 'us-east-1'", buildVariables())).isTrue();
  }

  @Test
  void evaluate_returnsFalse_whenHeaderValueDoesNotMatch() {
    assertThat(evaluator.evaluate("headers['X-Region'] == 'eu-west-1'", buildVariables()))
        .isFalse();
  }

  // --- param checks ---

  @Test
  void evaluate_returnsTrue_whenParamValueMatches() {
    assertThat(evaluator.evaluate("params['variant'] == 'B'", buildVariables())).isTrue();
  }

  @Test
  void evaluate_returnsFalse_whenParamValueDoesNotMatch() {
    assertThat(evaluator.evaluate("params['variant'] == 'A'", buildVariables())).isFalse();
  }

  // --- cookie checks ---

  @Test
  void evaluate_returnsTrue_whenCookieExists() {
    assertThat(evaluator.evaluate("cookies['session'] != null", buildVariables())).isTrue();
  }

  @Test
  void evaluate_returnsFalse_whenCookieDoesNotExist() {
    assertThat(evaluator.evaluate("cookies['missing'] != null", buildVariables())).isFalse();
  }

  // --- path checks ---

  @Test
  void evaluate_returnsTrue_whenPathMatches() {
    assertThat(evaluator.evaluate("path == '/api/v2/users'", buildVariables())).isTrue();
  }

  @Test
  void evaluate_returnsFalse_whenPathDoesNotMatch() {
    assertThat(evaluator.evaluate("path == '/api/v1/users'", buildVariables())).isFalse();
  }

  // --- method checks ---

  @Test
  void evaluate_returnsTrue_whenMethodMatches() {
    assertThat(evaluator.evaluate("method == 'POST'", buildVariables())).isTrue();
  }

  @Test
  void evaluate_returnsFalse_whenMethodDoesNotMatch() {
    assertThat(evaluator.evaluate("method == 'GET'", buildVariables())).isFalse();
  }

  // --- compound conditions ---

  @Test
  void evaluate_returnsTrue_whenCompoundConditionSatisfied() {
    assertThat(
            evaluator.evaluate(
                "headers['X-Beta'] != null && params['debug'] == 'true'", buildVariables()))
        .isTrue();
  }

  @Test
  void evaluate_returnsFalse_whenCompoundConditionPartiallyFails() {
    assertThat(
            evaluator.evaluate(
                "headers['X-Beta'] != null && params['variant'] == 'A'", buildVariables()))
        .isFalse();
  }

  // --- empty variables ---

  @Test
  void evaluate_returnsFalse_whenHeaderMapIsEmpty() {
    Map<String, Object> variables = buildVariables();
    variables.put("headers", new HashMap<>());
    assertThat(evaluator.evaluate("headers['X-Beta'] != null", variables)).isFalse();
  }

  // --- error handling with failOnError=true (fail-closed) ---

  @Test
  void evaluate_returnsFalse_whenExpressionIsInvalid_failOnErrorTrue() {
    assertThat(evaluator.evaluate("this is not valid SpEL !!!", buildVariables())).isFalse();
  }

  // --- error handling with failOnError=false (fail-open) ---

  @Test
  void evaluate_returnsTrue_whenExpressionIsInvalid_failOnErrorFalse() {
    SpelFeatureFlagConditionEvaluator failOpenEvaluator =
        new SpelFeatureFlagConditionEvaluator(false);
    assertThat(failOpenEvaluator.evaluate("this is not valid SpEL !!!", buildVariables())).isTrue();
  }

  // --- security: type reference and constructor injection blocked ---

  @Test
  void evaluate_returnsFalse_whenTypeReferenceAttempted() {
    // T(java.lang.Runtime).getRuntime().exec('...') should be rejected by SimpleEvaluationContext
    assertThat(
            evaluator.evaluate(
                "T(java.lang.Runtime).getRuntime().exec('whoami')", buildVariables()))
        .isFalse();
  }

  @Test
  void evaluate_returnsFalse_whenConstructorAttempted() {
    assertThat(evaluator.evaluate("new java.io.File('/etc/passwd')", buildVariables())).isFalse();
  }

  // --- expression caching ---

  @Test
  void evaluate_cachesExpression_acrossMultipleEvaluations() {
    Map<String, Object> variables = buildVariables();
    // Evaluate the same expression multiple times; should not throw
    for (int i = 0; i < 5; i++) {
      assertThat(evaluator.evaluate("headers['X-Beta'] != null", variables)).isTrue();
    }
  }
}
