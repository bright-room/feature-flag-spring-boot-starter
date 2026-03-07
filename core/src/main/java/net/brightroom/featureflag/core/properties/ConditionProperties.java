package net.brightroom.featureflag.core.properties;

/**
 * Properties for feature flag condition evaluation behavior.
 *
 * <p>Configuration example in {@code application.yml}:
 *
 * <pre>{@code
 * feature-flags:
 *   condition:
 *     fail-on-error: true
 * }</pre>
 *
 * <p>When {@code fail-on-error} is {@code true} (default), condition evaluation errors cause the
 * feature to be denied (fail-closed). When {@code false}, errors cause the condition check to be
 * skipped (fail-open).
 */
public class ConditionProperties {

  private boolean failOnError = true;

  /**
   * Returns whether condition evaluation errors should cause access denial.
   *
   * @return {@code true} for fail-closed (default), {@code false} for fail-open
   */
  public boolean failOnError() {
    return failOnError;
  }

  // for property binding
  void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }

  ConditionProperties() {}
}
