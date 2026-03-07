package net.brightroom.featureflag.core.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import net.brightroom.featureflag.core.condition.ConditionVariables;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import org.junit.jupiter.api.Test;

class EnabledEvaluationStepTest {

  private static final ConditionVariables EMPTY_VARS =
      new ConditionVariables(null, null, null, null, null, null);
  private static final EvaluationContext CTX =
      new EvaluationContext("my-feature", "", 100, EMPTY_VARS, () -> null);

  private final FeatureFlagProvider provider = mock(FeatureFlagProvider.class);
  private final EnabledEvaluationStep step = new EnabledEvaluationStep(provider);

  @Test
  void evaluate_returnsEmpty_whenFeatureEnabled() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    Optional<AccessDecision> result = step.evaluate(CTX);
    assertThat(result).isEmpty();
  }

  @Test
  void evaluate_returnsDenied_whenFeatureDisabled() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(false);
    Optional<AccessDecision> result = step.evaluate(CTX);
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(AccessDecision.Denied.class);
    AccessDecision.Denied denied = (AccessDecision.Denied) result.get();
    assertThat(denied.featureName()).isEqualTo("my-feature");
    assertThat(denied.reason()).isEqualTo(DeniedReason.DISABLED);
  }
}
