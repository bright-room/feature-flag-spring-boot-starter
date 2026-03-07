package net.brightroom.featureflag.core.evaluation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import net.brightroom.featureflag.core.condition.ConditionVariables;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import net.brightroom.featureflag.core.rollout.ReactiveRolloutStrategy;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactiveRolloutEvaluationStepTest {

  private static final ConditionVariables EMPTY_VARS =
      new ConditionVariables(null, null, null, null, null, null);
  private static final FeatureFlagContext FLAG_CTX = new FeatureFlagContext("user-1");

  private final ReactiveRolloutStrategy strategy = mock(ReactiveRolloutStrategy.class);
  private final ReactiveRolloutEvaluationStep step = new ReactiveRolloutEvaluationStep(strategy);

  @Test
  void evaluate_returnsAllowed_whenRolloutIs100() {
    EvaluationContext ctx =
        new EvaluationContext("my-feature", "", 100, EMPTY_VARS, () -> FLAG_CTX);
    StepVerifier.create(step.evaluate(ctx))
        .expectNextMatches(d -> d instanceof AccessDecision.Allowed)
        .verifyComplete();
    verifyNoInteractions(strategy);
  }

  @Test
  void evaluate_returnsAllowed_whenFlagContextIsNull_failOpen() {
    EvaluationContext ctx = new EvaluationContext("my-feature", "", 50, EMPTY_VARS, () -> null);
    StepVerifier.create(step.evaluate(ctx))
        .expectNextMatches(d -> d instanceof AccessDecision.Allowed)
        .verifyComplete();
    verifyNoInteractions(strategy);
  }

  @Test
  void evaluate_returnsAllowed_whenStrategyReturnsTrue() {
    when(strategy.isInRollout("my-feature", FLAG_CTX, 50)).thenReturn(Mono.just(true));
    EvaluationContext ctx = new EvaluationContext("my-feature", "", 50, EMPTY_VARS, () -> FLAG_CTX);
    StepVerifier.create(step.evaluate(ctx))
        .expectNextMatches(d -> d instanceof AccessDecision.Allowed)
        .verifyComplete();
  }

  @Test
  void evaluate_returnsDenied_whenStrategyReturnsFalse() {
    when(strategy.isInRollout("my-feature", FLAG_CTX, 50)).thenReturn(Mono.just(false));
    EvaluationContext ctx = new EvaluationContext("my-feature", "", 50, EMPTY_VARS, () -> FLAG_CTX);
    StepVerifier.create(step.evaluate(ctx))
        .expectNextMatches(
            d ->
                d instanceof AccessDecision.Denied denied
                    && denied.featureName().equals("my-feature")
                    && denied.reason() == DeniedReason.ROLLOUT_EXCLUDED)
        .verifyComplete();
  }
}
