package net.brightroom.featureflag.core.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import net.brightroom.featureflag.core.condition.ConditionVariables;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import net.brightroom.featureflag.core.provider.Schedule;
import net.brightroom.featureflag.core.provider.ScheduleProvider;
import org.junit.jupiter.api.Test;

class ScheduleEvaluationStepTest {

  private static final ConditionVariables EMPTY_VARS =
      new ConditionVariables(null, null, null, null, null, null);
  private static final EvaluationContext CTX =
      new EvaluationContext("my-feature", "", 100, EMPTY_VARS, null);

  // Fixed clock at 2025-06-15T12:00:00Z
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2025-06-15T12:00:00Z"), ZoneId.of("UTC"));

  private final ScheduleProvider scheduleProvider = mock(ScheduleProvider.class);
  private final ScheduleEvaluationStep step = new ScheduleEvaluationStep(scheduleProvider, CLOCK);

  @Test
  void evaluate_returnsEmpty_whenNoScheduleConfigured() {
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Optional.empty());
    assertThat(step.evaluate(CTX)).isEmpty();
  }

  @Test
  void evaluate_returnsEmpty_whenScheduleIsActive() {
    // start in past, no end → active
    Schedule active = new Schedule(LocalDateTime.of(2025, 1, 1, 0, 0), null, ZoneId.of("UTC"));
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Optional.of(active));
    assertThat(step.evaluate(CTX)).isEmpty();
  }

  @Test
  void evaluate_returnsDenied_whenScheduleIsInactive_endInPast() {
    // end in past → inactive
    Schedule inactive = new Schedule(null, LocalDateTime.of(2025, 1, 1, 0, 0), ZoneId.of("UTC"));
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Optional.of(inactive));

    Optional<AccessDecision> result = step.evaluate(CTX);
    assertThat(result).isPresent();
    AccessDecision.Denied denied = (AccessDecision.Denied) result.get();
    assertThat(denied.featureName()).isEqualTo("my-feature");
    assertThat(denied.reason()).isEqualTo(DeniedReason.SCHEDULE_INACTIVE);
  }

  @Test
  void evaluate_returnsDenied_whenScheduleIsInactive_startInFuture() {
    // start in future → inactive
    Schedule inactive = new Schedule(LocalDateTime.of(2025, 12, 1, 0, 0), null, ZoneId.of("UTC"));
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Optional.of(inactive));

    Optional<AccessDecision> result = step.evaluate(CTX);
    assertThat(result).isPresent();
    assertThat(((AccessDecision.Denied) result.get()).reason())
        .isEqualTo(DeniedReason.SCHEDULE_INACTIVE);
  }
}
