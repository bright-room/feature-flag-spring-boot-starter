package net.brightroom.featureflag.core.evaluation;

import java.time.Clock;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import net.brightroom.featureflag.core.provider.ReactiveScheduleProvider;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

/** Reactive evaluation step that checks whether the feature flag schedule is currently active. */
@Order(200)
public class ReactiveScheduleEvaluationStep implements ReactiveEvaluationStep {

  private final ReactiveScheduleProvider scheduleProvider;
  private final Clock clock;

  /**
   * Creates a new {@code ReactiveScheduleEvaluationStep}.
   *
   * @param scheduleProvider the provider used to look up the schedule per feature
   * @param clock the clock used to obtain the current time for schedule evaluation
   */
  public ReactiveScheduleEvaluationStep(ReactiveScheduleProvider scheduleProvider, Clock clock) {
    this.scheduleProvider = scheduleProvider;
    this.clock = clock;
  }

  @Override
  public Mono<AccessDecision> evaluate(EvaluationContext context) {
    return scheduleProvider
        .getSchedule(context.featureName())
        .map(
            schedule ->
                schedule.isActive(clock.instant())
                    ? AccessDecision.allowed()
                    : AccessDecision.denied(context.featureName(), DeniedReason.SCHEDULE_INACTIVE))
        .defaultIfEmpty(AccessDecision.allowed());
  }
}
