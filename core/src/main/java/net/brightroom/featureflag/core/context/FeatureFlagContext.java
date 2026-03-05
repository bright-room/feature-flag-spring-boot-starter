package net.brightroom.featureflag.core.context;

import java.util.Objects;

/**
 * Holds the context for a feature flag rollout check.
 *
 * <p>The {@code userIdentifier} is used to deterministically bucket requests into rollout groups.
 * For non-sticky (per-request random) rollout, implementations may pass a random UUID. For sticky
 * rollout (same user always gets the same result), pass a stable identifier such as a user ID or
 * session ID.
 *
 * @param userIdentifier a non-blank identifier used to bucket requests into rollout groups
 */
public record FeatureFlagContext(String userIdentifier) {
  /**
   * Validates that {@code userIdentifier} is not null or blank.
   *
   * @throws NullPointerException if {@code userIdentifier} is null
   * @throws IllegalArgumentException if {@code userIdentifier} is blank
   */
  public FeatureFlagContext {
    Objects.requireNonNull(userIdentifier, "userIdentifier must not be null");
    if (userIdentifier.isBlank()) {
      throw new IllegalArgumentException("userIdentifier must not be blank");
    }
  }
}
