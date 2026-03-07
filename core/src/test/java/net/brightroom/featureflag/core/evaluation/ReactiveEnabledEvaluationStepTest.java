package net.brightroom.featureflag.core.evaluation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.brightroom.featureflag.core.condition.ConditionVariables;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactiveEnabledEvaluationStepTest {

  private static final ConditionVariables EMPTY_VARS =
      new ConditionVariables(null, null, null, null, null, null);
  private static final EvaluationContext CTX =
      new EvaluationContext("my-feature", "", 100, EMPTY_VARS, null);

  private final ReactiveFeatureFlagProvider provider = mock(ReactiveFeatureFlagProvider.class);
  private final ReactiveEnabledEvaluationStep step = new ReactiveEnabledEvaluationStep(provider);

  @Test
  void evaluate_returnsAllowed_whenFeatureEnabled() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    StepVerifier.create(step.evaluate(CTX))
        .expectNextMatches(d -> d instanceof AccessDecision.Allowed)
        .verifyComplete();
  }

  @Test
  void evaluate_returnsDenied_whenFeatureDisabled() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(false));
    StepVerifier.create(step.evaluate(CTX))
        .expectNextMatches(
            d ->
                d instanceof AccessDecision.Denied denied
                    && denied.featureName().equals("my-feature")
                    && denied.reason() == DeniedReason.DISABLED)
        .verifyComplete();
  }

  @Test
  void evaluate_returnsDenied_whenProviderReturnsEmpty() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.empty());
    StepVerifier.create(step.evaluate(CTX))
        .expectNextMatches(
            d ->
                d instanceof AccessDecision.Denied denied
                    && denied.reason() == DeniedReason.DISABLED)
        .verifyComplete();
  }
}
