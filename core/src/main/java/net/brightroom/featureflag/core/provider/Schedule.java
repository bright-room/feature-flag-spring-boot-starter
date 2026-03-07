package net.brightroom.featureflag.core.provider;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.jspecify.annotations.Nullable;

/**
 * An immutable value object representing the schedule window of a feature flag.
 *
 * <p>Defines an optional {@code start} and {@code end} time (as {@link LocalDateTime}) and an
 * optional {@code timezone}. When {@link #isActive(Instant)} is called, the instant is converted to
 * the configured timezone (or system default if absent) and compared against the window.
 *
 * <p>This is the SPI return type for {@link ScheduleProvider} and {@link ReactiveScheduleProvider}.
 * Custom provider implementations should create instances directly:
 *
 * <pre>{@code
 * return Optional.of(new Schedule(startTime, endTime, ZoneId.of("Asia/Tokyo")));
 * }</pre>
 *
 * <ul>
 *   <li>{@code start} only — the feature is active from {@code start} onward
 *   <li>{@code end} only — the feature is active until {@code end}
 *   <li>both — the feature is active between {@code start} and {@code end} (inclusive)
 *   <li>neither — the feature is always active (equivalent to no schedule)
 *   <li>{@code timezone} omitted — system default timezone is used
 * </ul>
 *
 * @param start the schedule start time, or {@code null} if no start restriction is configured
 * @param end the schedule end time, or {@code null} if no end restriction is configured
 * @param timezone the timezone used to evaluate start/end times, or {@code null} if the system
 *     default timezone should be used
 */
public record Schedule(
    @Nullable LocalDateTime start, @Nullable LocalDateTime end, @Nullable ZoneId timezone) {

  public Schedule {
    if (start != null && end != null && start.isAfter(end)) {
      throw new IllegalArgumentException(
          "Schedule start must not be after end, but start=" + start + " end=" + end);
    }
  }

  /**
   * Returns whether the schedule window is active at the given instant.
   *
   * @param now the current instant to check; must not be null
   * @return {@code true} if {@code now} falls within the configured window, {@code false} otherwise
   */
  public boolean isActive(Instant now) {
    ZoneId zone = timezone != null ? timezone : ZoneId.systemDefault();
    LocalDateTime localNow = now.atZone(zone).toLocalDateTime();
    if (start != null && localNow.isBefore(start)) return false;
    if (end != null && localNow.isAfter(end)) return false;
    return true;
  }
}
