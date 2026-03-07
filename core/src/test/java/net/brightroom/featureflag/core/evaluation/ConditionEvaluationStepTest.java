package net.brightroom.featureflag.core.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import net.brightroom.featureflag.core.condition.ConditionVariables;
import net.brightroom.featureflag.core.condition.FeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import org.junit.jupiter.api.Test;

class ConditionEvaluationStepTest {

  private static final ConditionVariables EMPTY_VARS =
      new ConditionVariables(null, null, null, null, null, null);

  private final FeatureFlagConditionEvaluator evaluator = mock(FeatureFlagConditionEvaluator.class);
  private final ConditionEvaluationStep step = new ConditionEvaluationStep(evaluator);

  @Test
  void evaluate_returnsEmpty_whenConditionIsEmpty() {
    EvaluationContext ctx = new EvaluationContext("my-feature", "", 100, EMPTY_VARS, () -> null);
    assertThat(step.evaluate(ctx)).isEmpty();
    verifyNoInteractions(evaluator);
  }

  @Test
  void evaluate_returnsEmpty_whenConditionPasses() {
    when(evaluator.evaluate(eq("headers['X-Beta'] != null"), any())).thenReturn(true);
    EvaluationContext ctx =
        new EvaluationContext(
            "my-feature", "headers['X-Beta'] != null", 100, EMPTY_VARS, () -> null);
    assertThat(step.evaluate(ctx)).isEmpty();
  }

  @Test
  void evaluate_returnsDenied_whenConditionFails() {
    when(evaluator.evaluate(eq("headers['X-Beta'] != null"), any())).thenReturn(false);
    EvaluationContext ctx =
        new EvaluationContext(
            "my-feature", "headers['X-Beta'] != null", 100, EMPTY_VARS, () -> null);

    Optional<AccessDecision> result = step.evaluate(ctx);
    assertThat(result).isPresent();
    AccessDecision.Denied denied = (AccessDecision.Denied) result.get();
    assertThat(denied.featureName()).isEqualTo("my-feature");
    assertThat(denied.reason()).isEqualTo(DeniedReason.CONDITION_NOT_MET);
  }
}
