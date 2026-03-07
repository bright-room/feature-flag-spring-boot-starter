package net.brightroom.featureflag.core.properties;

/**
 * Configuration for a single feature flag, including its enabled status, rollout percentage,
 * optional condition expression, and optional schedule.
 *
 * <p>Used as the value type for {@code feature-flags.features} in {@link FeatureFlagProperties}.
 *
 * <p>Configuration example in {@code application.yml}:
 *
 * <pre>{@code
 * feature-flags:
 *   features:
 *     new-feature:
 *       enabled: true
 *       rollout: 50
 *       condition: "headers['X-Beta'] != null"
 *     christmas-sale:
 *       enabled: true
 *       schedule:
 *         start: "2026-12-25T00:00:00"
 *         end: "2027-01-05T23:59:59"
 *         timezone: "Asia/Tokyo"
 *     simple-feature:
 *       enabled: true
 * }</pre>
 */
public class FeatureProperties {

  private boolean enabled = true;
  private int rollout = 100;
  private String condition = "";
  private ScheduleProperties schedule;

  /**
   * Returns whether this feature is enabled.
   *
   * @return {@code true} if the feature is enabled, {@code false} otherwise
   */
  public boolean enabled() {
    return enabled;
  }

  /**
   * Returns the rollout percentage for this feature (0–100).
   *
   * <p>100 means fully enabled (all requests). 0 means effectively disabled (no requests even if
   * the flag is enabled).
   *
   * @return the rollout percentage
   */
  public int rollout() {
    return rollout;
  }

  /**
   * Returns the SpEL condition expression for this feature, or an empty string if no condition is
   * configured.
   *
   * @return the SpEL condition expression, or empty string
   */
  public String condition() {
    return condition;
  }

  /**
   * Returns the schedule configuration for this feature, or {@code null} if no schedule is
   * configured.
   *
   * @return the schedule configuration, or {@code null}
   */
  public ScheduleProperties schedule() {
    return schedule;
  }

  // for property binding
  void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  // for property binding
  void setRollout(int rollout) {
    if (rollout < 0 || rollout > 100) {
      throw new IllegalArgumentException("rollout must be between 0 and 100, but was: " + rollout);
    }
    this.rollout = rollout;
  }

  // for property binding
  void setCondition(String condition) {
    this.condition = condition != null ? condition : "";
  }

  // for property binding
  void setSchedule(ScheduleProperties schedule) {
    this.schedule = schedule;
  }

  FeatureProperties() {}
}
