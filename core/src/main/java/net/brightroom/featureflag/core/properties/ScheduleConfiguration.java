package net.brightroom.featureflag.core.properties;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Configuration for the schedule window of a single feature flag.
 *
 * <p>Defines an optional {@code start} and {@code end} time (as {@link LocalDateTime}) and an
 * optional {@code timezone}. When {@link #isActive(Instant)} is called, the instant is converted to
 * the configured timezone (or system default if absent) and compared against the window.
 *
 * <p>Configuration example in {@code application.yml}:
 *
 * <pre>{@code
 * feature-flags:
 *   features:
 *     christmas-sale:
 *       enabled: true
 *       schedule:
 *         start: "2026-12-25T00:00:00"
 *         end: "2027-01-05T23:59:59"
 *         timezone: "Asia/Tokyo"
 * }</pre>
 *
 * <ul>
 *   <li>{@code start} only — the feature is active from {@code start} onward
 *   <li>{@code end} only — the feature is active until {@code end}
 *   <li>both — the feature is active between {@code start} and {@code end} (inclusive)
 *   <li>neither — the feature is always active (equivalent to no schedule)
 *   <li>{@code timezone} omitted — system default timezone is used
 * </ul>
 */
public class ScheduleConfiguration {

  private LocalDateTime start;
  private LocalDateTime end;
  private ZoneId timezone;

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

  /**
   * Returns the schedule start time, or {@code null} if no start restriction is configured.
   *
   * @return the start time, or {@code null}
   */
  public LocalDateTime start() {
    return start;
  }

  /**
   * Returns the schedule end time, or {@code null} if no end restriction is configured.
   *
   * @return the end time, or {@code null}
   */
  public LocalDateTime end() {
    return end;
  }

  /**
   * Returns the timezone used to evaluate start/end times, or {@code null} if the system default
   * timezone should be used.
   *
   * @return the timezone, or {@code null}
   */
  public ZoneId timezone() {
    return timezone;
  }

  // for property binding
  void setStart(LocalDateTime start) {
    this.start = start;
  }

  // for property binding
  void setEnd(LocalDateTime end) {
    if (end != null && this.start != null && end.isBefore(this.start)) {
      throw new IllegalArgumentException(
          "schedule.end must not be before schedule.start, but end="
              + end
              + " start="
              + this.start);
    }
    this.end = end;
  }

  // for property binding
  void setTimezone(ZoneId timezone) {
    this.timezone = timezone;
  }

  ScheduleConfiguration() {}
}
