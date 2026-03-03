package net.brightroom.featureflag.core.context;

import java.util.Objects;

/**
 * Holds the context for a feature flag rollout check.
 *
 * <p>The {@code userIdentifier} is used to deterministically bucket requests into rollout groups.
 * For non-sticky (per-request random) rollout, implementations may pass a random UUID. For sticky
 * rollout (same user always gets the same result), pass a stable identifier such as a user ID or
 * session ID.
 */
public record FeatureFlagContext(String userIdentifier) {
  public FeatureFlagContext {
    Objects.requireNonNull(userIdentifier, "userIdentifier must not be null");
    if (userIdentifier.isBlank()) {
      throw new IllegalArgumentException("userIdentifier must not be blank");
    }
  }
}
