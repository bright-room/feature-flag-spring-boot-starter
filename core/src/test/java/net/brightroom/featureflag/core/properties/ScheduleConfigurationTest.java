package net.brightroom.featureflag.core.properties;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ScheduleConfigurationTest {

  private ScheduleConfiguration newSchedule() {
    return new ScheduleConfiguration();
  }

  // --- setEnd() validation ---

  @Test
  void setEnd_throwsIllegalArgumentException_whenEndIsBeforeStart() {
    ScheduleConfiguration schedule = newSchedule();
    schedule.setStart(LocalDateTime.of(2026, 6, 15, 12, 0));

    assertThatIllegalArgumentException()
        .isThrownBy(() -> schedule.setEnd(LocalDateTime.of(2026, 6, 15, 11, 0)))
        .withMessageContaining("schedule.end must not be before schedule.start");
  }

  @Test
  void setEnd_doesNotThrow_whenEndEqualsStart() {
    ScheduleConfiguration schedule = newSchedule();
    schedule.setStart(LocalDateTime.of(2026, 6, 15, 12, 0));

    assertThatNoException().isThrownBy(() -> schedule.setEnd(LocalDateTime.of(2026, 6, 15, 12, 0)));
  }

  @Test
  void setEnd_doesNotThrow_whenEndIsAfterStart() {
    ScheduleConfiguration schedule = newSchedule();
    schedule.setStart(LocalDateTime.of(2026, 6, 15, 12, 0));

    assertThatNoException().isThrownBy(() -> schedule.setEnd(LocalDateTime.of(2026, 6, 15, 13, 0)));
  }

  @Test
  void setEnd_doesNotThrow_whenStartIsNull() {
    ScheduleConfiguration schedule = newSchedule();

    assertThatNoException().isThrownBy(() -> schedule.setEnd(LocalDateTime.of(2026, 6, 15, 11, 0)));
  }

  @Test
  void setEnd_doesNotThrow_whenEndIsNull() {
    ScheduleConfiguration schedule = newSchedule();
    schedule.setStart(LocalDateTime.of(2026, 6, 15, 12, 0));

    assertThatNoException().isThrownBy(() -> schedule.setEnd(null));
  }

  // --- setStart() validation (H-1: order-independent validation) ---

  @Test
  void setStart_throwsIllegalArgumentException_whenStartIsAfterEnd() {
    ScheduleConfiguration schedule = newSchedule();
    schedule.setEnd(LocalDateTime.of(2026, 6, 15, 12, 0));

    assertThatIllegalArgumentException()
        .isThrownBy(() -> schedule.setStart(LocalDateTime.of(2026, 6, 15, 13, 0)))
        .withMessageContaining("schedule.start must not be after schedule.end");
  }

  @Test
  void setStart_doesNotThrow_whenStartEqualsEnd() {
    ScheduleConfiguration schedule = newSchedule();
    schedule.setEnd(LocalDateTime.of(2026, 6, 15, 12, 0));

    assertThatNoException()
        .isThrownBy(() -> schedule.setStart(LocalDateTime.of(2026, 6, 15, 12, 0)));
  }

  @Test
  void setStart_doesNotThrow_whenStartIsBeforeEnd() {
    ScheduleConfiguration schedule = newSchedule();
    schedule.setEnd(LocalDateTime.of(2026, 6, 15, 12, 0));

    assertThatNoException()
        .isThrownBy(() -> schedule.setStart(LocalDateTime.of(2026, 6, 15, 11, 0)));
  }

  @Test
  void setStart_doesNotThrow_whenEndIsNull() {
    ScheduleConfiguration schedule = newSchedule();

    assertThatNoException()
        .isThrownBy(() -> schedule.setStart(LocalDateTime.of(2026, 6, 15, 12, 0)));
  }

  @Test
  void setStart_doesNotThrow_whenStartIsNull() {
    ScheduleConfiguration schedule = newSchedule();
    schedule.setEnd(LocalDateTime.of(2026, 6, 15, 12, 0));

    assertThatNoException().isThrownBy(() -> schedule.setStart(null));
  }

  // --- toSchedule() ---

  @Test
  void toSchedule_returnsScheduleWithSameValues() {
    ScheduleConfiguration config = newSchedule();
    config.setStart(LocalDateTime.of(2026, 6, 15, 10, 0));
    config.setEnd(LocalDateTime.of(2026, 6, 15, 18, 0));

    var schedule = config.toSchedule();

    assertEquals(LocalDateTime.of(2026, 6, 15, 10, 0), schedule.start());
    assertEquals(LocalDateTime.of(2026, 6, 15, 18, 0), schedule.end());
    assertNull(schedule.timezone());
  }
}
