package net.brightroom.featureflag.core.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpelReactiveFeatureFlagConditionEvaluatorTest {

  @Mock FeatureFlagConditionEvaluator delegate;

  @Test
  void evaluate_returnsTrue_whenDelegateReturnsTrue() {
    Map<String, Object> variables = Map.of("method", "GET");
    when(delegate.evaluate("method == 'GET'", variables)).thenReturn(true);

    SpelReactiveFeatureFlagConditionEvaluator evaluator =
        new SpelReactiveFeatureFlagConditionEvaluator(delegate);

    Boolean result = evaluator.evaluate("method == 'GET'", variables).block();

    assertThat(result).isTrue();
    verify(delegate).evaluate("method == 'GET'", variables);
  }

  @Test
  void evaluate_returnsFalse_whenDelegateReturnsFalse() {
    Map<String, Object> variables = Map.of("method", "POST");
    when(delegate.evaluate("method == 'GET'", variables)).thenReturn(false);

    SpelReactiveFeatureFlagConditionEvaluator evaluator =
        new SpelReactiveFeatureFlagConditionEvaluator(delegate);

    Boolean result = evaluator.evaluate("method == 'GET'", variables).block();

    assertThat(result).isFalse();
    verify(delegate).evaluate("method == 'GET'", variables);
  }
}
