package net.brightroom.featureflag.core.exception;

/**
 * Thrown when access to a feature-flagged endpoint is denied.
 *
 * <p>This exception can be caught by a {@code @ControllerAdvice} to customize the response. If not
 * handled, the library's default response behavior applies.
 */
public class FeatureFlagAccessDeniedException extends RuntimeException {

  String featureName;

  /**
   * Constructor.
   *
   * @param featureName the name of the feature that is not available
   */
  public FeatureFlagAccessDeniedException(String featureName) {
    super(String.format("Feature '%s' is not available", featureName));
    this.featureName = featureName;
  }

  /**
   * Returns the name of the feature that is not available.
   *
   * @return the name of the feature
   */
  public String featureName() {
    return featureName;
  }
}
