package net.brightroom.featureflag.filter;

/**
 * Exception thrown when a feature flag check fails. This exception is thrown when attempting to
 * access a disabled feature that is marked as required.
 */
public class FeatureFlagDisabledException extends RuntimeException {

  /**
   * Creates a new FeatureFlagDisabledException with the specified message.
   *
   * @param message the detail message
   */
  public FeatureFlagDisabledException(String message) {
    super(message);
  }
}
