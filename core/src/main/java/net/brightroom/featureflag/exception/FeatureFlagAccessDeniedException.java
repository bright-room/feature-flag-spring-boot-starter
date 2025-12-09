package net.brightroom.featureflag.exception;

/** Exception thrown when a feature flag is accessed but is not enabled. */
public class FeatureFlagAccessDeniedException extends RuntimeException {

  /**
   * Constructor.
   *
   * @param message the detail message
   */
  public FeatureFlagAccessDeniedException(String message) {
    super(message);
  }
}
