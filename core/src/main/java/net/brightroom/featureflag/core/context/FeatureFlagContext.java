package net.brightroom.featureflag.core.context;

/**
 * Holds the user identity used for rollout bucket assignment.
 *
 * <p>{@code userId} is the key that drives the deterministic, hash-based bucket computation. The
 * same {@code userId} always maps to the same bucket for a given feature name, ensuring that a user
 * consistently receives the same access decision across requests (sticky rollout).
 *
 * <p>Implementations of {@code FeatureFlagContextResolver} are responsible for extracting the
 * {@code userId} from the current request (e.g., from a session, a JWT claim, or a custom header).
 */
public record FeatureFlagContext(String userId) {

  /**
   * Creates a {@code FeatureFlagContext} with the given user identifier.
   *
   * @param userId the user identifier; must not be {@code null} or blank
   * @throws IllegalArgumentException if {@code userId} is {@code null} or blank
   */
  public FeatureFlagContext {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("userId must not be null or blank");
    }
  }
}
