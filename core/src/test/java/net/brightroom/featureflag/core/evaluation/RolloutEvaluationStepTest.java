package net.brightroom.featureflag.core.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import net.brightroom.featureflag.core.condition.ConditionVariables;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import net.brightroom.featureflag.core.rollout.RolloutStrategy;
import org.junit.jupiter.api.Test;

class RolloutEvaluationStepTest {

  private static final ConditionVariables EMPTY_VARS =
      new ConditionVariables(null, null, null, null, null, null);
  private static final FeatureFlagContext CTX = new FeatureFlagContext("user-1");

  private final RolloutStrategy strategy = mock(RolloutStrategy.class);
  private final RolloutEvaluationStep step = new RolloutEvaluationStep(strategy);

  @Test
  void evaluate_returnsEmpty_whenRolloutIs100() {
    EvaluationContext ctx = new EvaluationContext("my-feature", "", 100, EMPTY_VARS, CTX);
    assertThat(step.evaluate(ctx)).isEmpty();
    verifyNoInteractions(strategy);
  }

  @Test
  void evaluate_returnsEmpty_whenFlagContextIsNull_failOpen() {
    EvaluationContext ctx = new EvaluationContext("my-feature", "", 50, EMPTY_VARS, null);
    assertThat(step.evaluate(ctx)).isEmpty();
    verifyNoInteractions(strategy);
  }

  @Test
  void evaluate_returnsEmpty_whenStrategyReturnsTrue() {
    when(strategy.isInRollout("my-feature", CTX, 50)).thenReturn(true);
    EvaluationContext ctx = new EvaluationContext("my-feature", "", 50, EMPTY_VARS, CTX);
    assertThat(step.evaluate(ctx)).isEmpty();
  }

  @Test
  void evaluate_returnsDenied_whenStrategyReturnsFalse() {
    when(strategy.isInRollout("my-feature", CTX, 50)).thenReturn(false);
    EvaluationContext ctx = new EvaluationContext("my-feature", "", 50, EMPTY_VARS, CTX);

    Optional<AccessDecision> result = step.evaluate(ctx);
    assertThat(result).isPresent();
    AccessDecision.Denied denied = (AccessDecision.Denied) result.get();
    assertThat(denied.featureName()).isEqualTo("my-feature");
    assertThat(denied.reason()).isEqualTo(DeniedReason.ROLLOUT_EXCLUDED);
  }
}
