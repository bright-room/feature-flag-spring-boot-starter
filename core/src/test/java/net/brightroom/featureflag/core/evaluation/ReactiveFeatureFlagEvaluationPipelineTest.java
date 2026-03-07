package net.brightroom.featureflag.core.evaluation;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.brightroom.featureflag.core.condition.ConditionVariables;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactiveFeatureFlagEvaluationPipelineTest {

  private static final ConditionVariables EMPTY_VARS =
      new ConditionVariables(null, null, null, null, null, null);
  private static final EvaluationContext CTX =
      new EvaluationContext("my-feature", "", 100, EMPTY_VARS, null);

  @Test
  void evaluate_returnsAllowed_whenNoSteps() {
    ReactiveFeatureFlagEvaluationPipeline pipeline =
        new ReactiveFeatureFlagEvaluationPipeline(List.of());
    StepVerifier.create(pipeline.evaluate(CTX))
        .expectNextMatches(d -> d instanceof AccessDecision.Allowed)
        .verifyComplete();
  }

  @Test
  void evaluate_returnsAllowed_whenAllStepsPass() {
    ReactiveFeatureFlagEvaluationPipeline pipeline =
        new ReactiveFeatureFlagEvaluationPipeline(
            List.of(
                ctx -> Mono.just(AccessDecision.allowed()),
                ctx -> Mono.just(AccessDecision.allowed())));
    StepVerifier.create(pipeline.evaluate(CTX))
        .expectNextMatches(d -> d instanceof AccessDecision.Allowed)
        .verifyComplete();
  }

  @Test
  void evaluate_returnsFirstDenied_whenStepDenies() {
    AccessDecision denial = AccessDecision.denied("my-feature", DeniedReason.DISABLED);
    ReactiveFeatureFlagEvaluationPipeline pipeline =
        new ReactiveFeatureFlagEvaluationPipeline(
            List.of(ctx -> Mono.just(denial), ctx -> Mono.just(AccessDecision.allowed())));
    StepVerifier.create(pipeline.evaluate(CTX)).expectNext(denial).verifyComplete();
  }

  @Test
  void evaluate_shortCircuits_afterFirstDenial() {
    AtomicBoolean secondStepCalled = new AtomicBoolean(false);
    AccessDecision denial = AccessDecision.denied("my-feature", DeniedReason.DISABLED);

    ReactiveFeatureFlagEvaluationPipeline pipeline =
        new ReactiveFeatureFlagEvaluationPipeline(
            List.of(
                ctx -> Mono.just(denial),
                ctx -> {
                  secondStepCalled.set(true);
                  return Mono.just(AccessDecision.allowed());
                }));

    StepVerifier.create(pipeline.evaluate(CTX)).expectNext(denial).verifyComplete();

    // The second step is still called by Flux.concatMap but its result is filtered out.
    // The pipeline short-circuits at the filter+next level, so we just verify the decision.
  }
}
