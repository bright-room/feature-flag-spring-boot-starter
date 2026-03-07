package net.brightroom.featureflag.core.evaluation;

/**
 * Represents the outcome of a feature flag evaluation pipeline.
 *
 * <p>Either {@link Allowed} (proceed) or {@link Denied} (block with a reason).
 */
public sealed interface AccessDecision {

  /** Indicates that access is allowed. */
  record Allowed() implements AccessDecision {}

  /** Indicates that access is denied, with the feature name and reason. */
  record Denied(String featureName, DeniedReason reason) implements AccessDecision {}

  /** Reason for denying access in the evaluation pipeline. */
  enum DeniedReason {
    /** The feature flag is disabled. */
    DISABLED,
    /** The feature flag has a schedule that is not currently active. */
    SCHEDULE_INACTIVE,
    /** The SpEL condition evaluated to false. */
    CONDITION_NOT_MET,
    /** The request is outside the rollout bucket. */
    ROLLOUT_EXCLUDED
  }

  /**
   * Returns an {@link Allowed} decision.
   *
   * @return a new {@code Allowed} instance
   */
  static AccessDecision allowed() {
    return new Allowed();
  }

  /**
   * Returns a {@link Denied} decision.
   *
   * @param featureName the feature flag name that was denied
   * @param reason the reason for denial
   * @return a new {@code Denied} instance
   */
  static AccessDecision denied(String featureName, DeniedReason reason) {
    return new Denied(featureName, reason);
  }
}
