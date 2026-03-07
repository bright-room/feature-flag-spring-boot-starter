package net.brightroom.featureflag.core.evaluation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import net.brightroom.featureflag.core.condition.ConditionVariables;
import net.brightroom.featureflag.core.condition.ReactiveFeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactiveConditionEvaluationStepTest {

  private static final ConditionVariables EMPTY_VARS =
      new ConditionVariables(null, null, null, null, null, null);

  private final ReactiveFeatureFlagConditionEvaluator evaluator =
      mock(ReactiveFeatureFlagConditionEvaluator.class);
  private final ReactiveConditionEvaluationStep step =
      new ReactiveConditionEvaluationStep(evaluator);

  @Test
  void evaluate_returnsAllowed_whenConditionIsEmpty() {
    EvaluationContext ctx = new EvaluationContext("my-feature", "", 100, EMPTY_VARS, null);
    StepVerifier.create(step.evaluate(ctx))
        .expectNextMatches(d -> d instanceof AccessDecision.Allowed)
        .verifyComplete();
    verifyNoInteractions(evaluator);
  }

  @Test
  void evaluate_returnsAllowed_whenConditionPasses() {
    when(evaluator.evaluate(eq("headers['X-Beta'] != null"), any())).thenReturn(Mono.just(true));
    EvaluationContext ctx =
        new EvaluationContext("my-feature", "headers['X-Beta'] != null", 100, EMPTY_VARS, null);
    StepVerifier.create(step.evaluate(ctx))
        .expectNextMatches(d -> d instanceof AccessDecision.Allowed)
        .verifyComplete();
  }

  @Test
  void evaluate_returnsDenied_whenConditionFails() {
    when(evaluator.evaluate(eq("headers['X-Beta'] != null"), any())).thenReturn(Mono.just(false));
    EvaluationContext ctx =
        new EvaluationContext("my-feature", "headers['X-Beta'] != null", 100, EMPTY_VARS, null);
    StepVerifier.create(step.evaluate(ctx))
        .expectNextMatches(
            d ->
                d instanceof AccessDecision.Denied denied
                    && denied.featureName().equals("my-feature")
                    && denied.reason() == DeniedReason.CONDITION_NOT_MET)
        .verifyComplete();
  }
}
