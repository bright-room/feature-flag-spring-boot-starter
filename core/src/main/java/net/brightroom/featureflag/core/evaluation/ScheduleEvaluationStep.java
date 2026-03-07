package net.brightroom.featureflag.core.evaluation;

import java.time.Clock;
import java.util.Optional;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import net.brightroom.featureflag.core.provider.ScheduleProvider;
import org.springframework.core.annotation.Order;

/** Evaluation step that checks whether the feature flag schedule is currently active. */
@Order(200)
public class ScheduleEvaluationStep implements EvaluationStep {

  private final ScheduleProvider scheduleProvider;
  private final Clock clock;

  /**
   * Creates a new {@code ScheduleEvaluationStep}.
   *
   * @param scheduleProvider the provider used to look up the schedule per feature
   * @param clock the clock used to obtain the current time for schedule evaluation
   */
  public ScheduleEvaluationStep(ScheduleProvider scheduleProvider, Clock clock) {
    this.scheduleProvider = scheduleProvider;
    this.clock = clock;
  }

  @Override
  public Optional<AccessDecision> evaluate(EvaluationContext context) {
    return scheduleProvider
        .getSchedule(context.featureName())
        .filter(schedule -> !schedule.isActive(clock.instant()))
        .map(
            schedule ->
                AccessDecision.denied(context.featureName(), DeniedReason.SCHEDULE_INACTIVE));
  }
}
