package net.brightroom.featureflag.core.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ScheduleTest {

  // Fixed reference instant: 2026-06-15T12:00:00 UTC
  private final Instant now = Instant.parse("2026-06-15T12:00:00Z");

  // --- start only ---

  @Test
  void isActive_returnsFalse_whenNowIsBeforeStart() {
    // start is in the future relative to now
    Schedule schedule = new Schedule(LocalDateTime.of(2026, 6, 15, 13, 0), null, ZoneId.of("UTC"));

    assertFalse(schedule.isActive(now));
  }

  @Test
  void isActive_returnsTrue_whenNowIsAfterStart() {
    // start is in the past relative to now
    Schedule schedule = new Schedule(LocalDateTime.of(2026, 6, 15, 11, 0), null, ZoneId.of("UTC"));

    assertTrue(schedule.isActive(now));
  }

  // --- end only ---

  @Test
  void isActive_returnsTrue_whenNowIsBeforeEnd() {
    Schedule schedule = new Schedule(null, LocalDateTime.of(2026, 6, 15, 13, 0), ZoneId.of("UTC"));

    assertTrue(schedule.isActive(now));
  }

  @Test
  void isActive_returnsFalse_whenNowIsAfterEnd() {
    Schedule schedule = new Schedule(null, LocalDateTime.of(2026, 6, 15, 11, 0), ZoneId.of("UTC"));

    assertFalse(schedule.isActive(now));
  }

  // --- start + end ---

  @Test
  void isActive_returnsTrue_whenNowIsWithinRange() {
    Schedule schedule =
        new Schedule(
            LocalDateTime.of(2026, 6, 15, 11, 0),
            LocalDateTime.of(2026, 6, 15, 13, 0),
            ZoneId.of("UTC"));

    assertTrue(schedule.isActive(now));
  }

  @Test
  void isActive_returnsFalse_whenNowIsBeforeRange() {
    Schedule schedule =
        new Schedule(
            LocalDateTime.of(2026, 6, 15, 13, 0),
            LocalDateTime.of(2026, 6, 15, 15, 0),
            ZoneId.of("UTC"));

    assertFalse(schedule.isActive(now));
  }

  @Test
  void isActive_returnsFalse_whenNowIsAfterRange() {
    Schedule schedule =
        new Schedule(
            LocalDateTime.of(2026, 6, 15, 9, 0),
            LocalDateTime.of(2026, 6, 15, 11, 0),
            ZoneId.of("UTC"));

    assertFalse(schedule.isActive(now));
  }

  // --- neither start nor end ---

  @Test
  void isActive_returnsTrue_whenBothStartAndEndAreNull() {
    Schedule schedule = new Schedule(null, null, null);

    assertTrue(schedule.isActive(now));
  }

  // --- timezone ---

  @Test
  void isActive_appliesTimezone_whenTimezoneIsConfigured() {
    // now = 2026-06-15T12:00:00 UTC = 2026-06-15T21:00:00 JST (UTC+9)
    // schedule: active from 20:00 JST, so 12:00 UTC is after start → active
    Schedule schedule =
        new Schedule(LocalDateTime.of(2026, 6, 15, 20, 0), null, ZoneId.of("Asia/Tokyo"));

    assertTrue(schedule.isActive(now));
  }

  @Test
  void isActive_returnsFalse_usingTimezone_whenBeforeStart() {
    // now = 2026-06-15T12:00:00 UTC = 2026-06-15T21:00:00 JST
    // schedule: active from 22:00 JST → not yet active
    Schedule schedule =
        new Schedule(LocalDateTime.of(2026, 6, 15, 22, 0), null, ZoneId.of("Asia/Tokyo"));

    assertFalse(schedule.isActive(now));
  }

  @Test
  void isActive_usesSystemDefaultTimezone_whenTimezoneIsNull() {
    // With null timezone, ZoneId.systemDefault() is used; verify it doesn't throw
    Schedule schedule = new Schedule(null, null, null);

    assertTrue(schedule.isActive(now));
  }

  // --- boundary values ---

  @Test
  void isActive_returnsTrue_whenNowEqualsStart() {
    // now = 2026-06-15T12:00:00 UTC, start = 2026-06-15T12:00:00 UTC
    // localNow.isBefore(start) is false at exact equality, so active
    Schedule schedule = new Schedule(LocalDateTime.of(2026, 6, 15, 12, 0), null, ZoneId.of("UTC"));

    assertTrue(schedule.isActive(now));
  }

  @Test
  void isActive_returnsTrue_whenNowEqualsEnd() {
    // now = 2026-06-15T12:00:00 UTC, end = 2026-06-15T12:00:00 UTC
    // localNow.isAfter(end) is false at exact equality, so active
    Schedule schedule = new Schedule(null, LocalDateTime.of(2026, 6, 15, 12, 0), ZoneId.of("UTC"));

    assertTrue(schedule.isActive(now));
  }

  // --- explicit UTC offset ---

  @Test
  void isActive_worksWithExplicitUtcOffset() {
    // now = 2026-06-15T12:00:00 UTC = 2026-06-15T09:00:00 UTC-3
    // schedule: active from 08:00 UTC-3 → active
    Schedule schedule =
        new Schedule(
            LocalDateTime.of(2026, 6, 15, 8, 0), null, ZoneId.from(ZoneOffset.ofHours(-3)));

    assertTrue(schedule.isActive(now));
  }
}
