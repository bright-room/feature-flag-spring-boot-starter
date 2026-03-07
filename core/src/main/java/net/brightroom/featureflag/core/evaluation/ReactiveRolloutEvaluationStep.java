package net.brightroom.featureflag.core.evaluation;

import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import net.brightroom.featureflag.core.rollout.ReactiveRolloutStrategy;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

/** Reactive evaluation step that checks whether the request is within the rollout bucket. */
@Order(400)
public class ReactiveRolloutEvaluationStep implements ReactiveEvaluationStep {

  private final ReactiveRolloutStrategy rolloutStrategy;

  /**
   * Creates a new {@code ReactiveRolloutEvaluationStep}.
   *
   * @param rolloutStrategy the strategy used to determine rollout bucket membership
   */
  public ReactiveRolloutEvaluationStep(ReactiveRolloutStrategy rolloutStrategy) {
    this.rolloutStrategy = rolloutStrategy;
  }

  @Override
  public Mono<AccessDecision> evaluate(EvaluationContext context) {
    if (context.rolloutPercentage() >= 100) {
      return Mono.just(AccessDecision.allowed());
    }
    if (context.flagContext() == null) {
      return Mono.just(AccessDecision.allowed()); // fail-open: no context available
    }
    return rolloutStrategy
        .isInRollout(context.featureName(), context.flagContext(), context.rolloutPercentage())
        .map(
            inRollout ->
                inRollout
                    ? AccessDecision.allowed()
                    : AccessDecision.denied(context.featureName(), DeniedReason.ROLLOUT_EXCLUDED));
  }
}
