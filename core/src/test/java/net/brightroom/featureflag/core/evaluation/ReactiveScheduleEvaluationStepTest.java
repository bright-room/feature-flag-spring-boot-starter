package net.brightroom.featureflag.core.evaluation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import net.brightroom.featureflag.core.condition.ConditionVariables;
import net.brightroom.featureflag.core.evaluation.AccessDecision.DeniedReason;
import net.brightroom.featureflag.core.provider.ReactiveScheduleProvider;
import net.brightroom.featureflag.core.provider.Schedule;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactiveScheduleEvaluationStepTest {

  private static final ConditionVariables EMPTY_VARS =
      new ConditionVariables(null, null, null, null, null, null);
  private static final EvaluationContext CTX =
      new EvaluationContext("my-feature", "", 100, EMPTY_VARS, null);

  // Fixed clock at 2025-06-15T12:00:00Z
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2025-06-15T12:00:00Z"), ZoneId.of("UTC"));

  private final ReactiveScheduleProvider scheduleProvider = mock(ReactiveScheduleProvider.class);
  private final ReactiveScheduleEvaluationStep step =
      new ReactiveScheduleEvaluationStep(scheduleProvider, CLOCK);

  @Test
  void evaluate_returnsAllowed_whenNoScheduleConfigured() {
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Mono.empty());
    StepVerifier.create(step.evaluate(CTX))
        .expectNextMatches(d -> d instanceof AccessDecision.Allowed)
        .verifyComplete();
  }

  @Test
  void evaluate_returnsAllowed_whenScheduleIsActive() {
    // start in past, no end → active
    Schedule active = new Schedule(LocalDateTime.of(2025, 1, 1, 0, 0), null, ZoneId.of("UTC"));
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Mono.just(active));
    StepVerifier.create(step.evaluate(CTX))
        .expectNextMatches(d -> d instanceof AccessDecision.Allowed)
        .verifyComplete();
  }

  @Test
  void evaluate_returnsDenied_whenScheduleIsInactive_endInPast() {
    // end in past → inactive
    Schedule inactive = new Schedule(null, LocalDateTime.of(2025, 1, 1, 0, 0), ZoneId.of("UTC"));
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Mono.just(inactive));
    StepVerifier.create(step.evaluate(CTX))
        .expectNextMatches(
            d ->
                d instanceof AccessDecision.Denied denied
                    && denied.featureName().equals("my-feature")
                    && denied.reason() == DeniedReason.SCHEDULE_INACTIVE)
        .verifyComplete();
  }

  @Test
  void evaluate_returnsDenied_whenScheduleIsInactive_startInFuture() {
    // start in future → inactive
    Schedule inactive = new Schedule(LocalDateTime.of(2025, 12, 1, 0, 0), null, ZoneId.of("UTC"));
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Mono.just(inactive));
    StepVerifier.create(step.evaluate(CTX))
        .expectNextMatches(
            d ->
                d instanceof AccessDecision.Denied denied
                    && denied.reason() == DeniedReason.SCHEDULE_INACTIVE)
        .verifyComplete();
  }
}
