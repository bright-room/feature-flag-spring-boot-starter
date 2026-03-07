package net.brightroom.featureflag.core.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import net.brightroom.featureflag.core.condition.ConditionVariables;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import org.junit.jupiter.api.Test;

class FeatureFlagEvaluationPipelineTest {

  private static final ConditionVariables EMPTY_VARS =
      new ConditionVariables(null, null, null, null, null, null);
  private static final EvaluationContext CTX =
      new EvaluationContext("my-feature", "", 100, EMPTY_VARS, () -> null);

  @Test
  void evaluate_returnsAllowed_whenNoSteps() {
    FeatureFlagEvaluationPipeline pipeline = new FeatureFlagEvaluationPipeline(List.of());
    assertThat(pipeline.evaluate(CTX)).isInstanceOf(AccessDecision.Allowed.class);
  }

  @Test
  void evaluate_returnsAllowed_whenAllStepsPass() {
    FeatureFlagEvaluationPipeline pipeline =
        new FeatureFlagEvaluationPipeline(
            List.of(ctx -> Optional.empty(), ctx -> Optional.empty()));
    assertThat(pipeline.evaluate(CTX)).isInstanceOf(AccessDecision.Allowed.class);
  }

  @Test
  void evaluate_returnsFirstDenied_whenStepDenies() {
    AccessDecision expectedDenial = AccessDecision.denied("my-feature", DeniedReason.DISABLED);
    FeatureFlagEvaluationPipeline pipeline =
        new FeatureFlagEvaluationPipeline(
            List.of(ctx -> Optional.of(expectedDenial), ctx -> Optional.empty()));
    assertThat(pipeline.evaluate(CTX)).isEqualTo(expectedDenial);
  }

  @Test
  void evaluate_shortCircuits_afterFirstDenial() {
    boolean[] secondStepCalled = {false};
    AccessDecision denial = AccessDecision.denied("my-feature", DeniedReason.DISABLED);

    FeatureFlagEvaluationPipeline pipeline =
        new FeatureFlagEvaluationPipeline(
            List.of(
                ctx -> Optional.of(denial),
                ctx -> {
                  secondStepCalled[0] = true;
                  return Optional.empty();
                }));

    pipeline.evaluate(CTX);
    assertThat(secondStepCalled[0]).isFalse();
  }
}
